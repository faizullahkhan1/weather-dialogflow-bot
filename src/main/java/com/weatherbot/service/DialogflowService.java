package com.weatherbot.service;

import com.weatherbot.model.CurrentWeatherResponse;
import com.weatherbot.model.DialogflowRequest;
import com.weatherbot.model.DialogflowResponse;
import com.weatherbot.model.ForecastWeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Core service that processes incoming Dialogflow webhook requests.
 *
 * Intent routing:
 *   - "weather.current"  → Handles current weather queries
 *   - "weather.forecast" → Handles forecast queries (up to 8 days)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogflowService {

    private final WeatherApiService weatherApiService;

    // Intent display names as configured in Dialogflow ES console
    private static final String INTENT_CURRENT_WEATHER = "weather.current";
    private static final String INTENT_FORECAST_WEATHER = "weather.forecast";

    /**
     * Main entry point. Routes the request to the appropriate handler
     * based on the detected intent action name.
     */
    public DialogflowResponse handleWebhook(DialogflowRequest request) {
        if (request == null || request.getQueryResult() == null) {
            log.warn("Received null or malformed Dialogflow request");
            return DialogflowResponse.of("Sorry, I encountered an error processing your request.");
        }

        String action = request.getQueryResult().getAction();
        String intentName = request.getQueryResult().getIntent() != null
                ? request.getQueryResult().getIntent().getDisplayName()
                : "";

        log.info("Received webhook - Action: {}, Intent: {}", action, intentName);
        log.debug("Parameters: {}", request.getQueryResult().getParameters());

        // Route by action name (set in Dialogflow intent configuration)
        if (INTENT_CURRENT_WEATHER.equals(action) || intentName.toLowerCase().contains("current")) {
            return handleCurrentWeather(request.getQueryResult().getParameters());
        } else if (INTENT_FORECAST_WEATHER.equals(action) || intentName.toLowerCase().contains("forecast")) {
            return handleForecastWeather(request.getQueryResult().getParameters());
        } else {
            log.warn("Unknown action/intent: {} / {}", action, intentName);
            return DialogflowResponse.of("I'm not sure what weather information you need. " +
                    "You can ask me for current weather or a forecast!");
        }
    }

    /**
     * Handles current weather intent.
     * Extracts "geo-city" parameter from Dialogflow and fetches current weather.
     */
    private DialogflowResponse handleCurrentWeather(Map<String, Object> parameters) {
        String city = extractCity(parameters);

        if (city == null || city.isBlank()) {
            return DialogflowResponse.of("Please provide a city name so I can check the weather for you.");
        }

        try {
            log.info("Fetching current weather for: {}", city);
            CurrentWeatherResponse weatherData = weatherApiService.getCurrentWeather(city);
            String response = weatherApiService.formatCurrentWeather(weatherData);
            return DialogflowResponse.of(response);

        } catch (Exception e) {
            log.error("Error fetching current weather for city: {}", city, e);
            return DialogflowResponse.of(
                    String.format("Sorry, I couldn't find weather data for '%s'. " +
                            "Please check the city name and try again.", city));
        }
    }

    /**
     * Handles weather forecast intent.
     * Extracts "geo-city" and "date" parameters. Date defaults to today.
     * Calculates a range from startDate to startDate + 8 days.
     */
    private DialogflowResponse handleForecastWeather(Map<String, Object> parameters) {
        String city = extractCity(parameters);

        if (city == null || city.isBlank()) {
            return DialogflowResponse.of("Please provide a city name for the forecast.");
        }

        // Extract the start date; default to today if not provided
        LocalDate startDate = extractDate(parameters);
        LocalDate endDate = startDate.plusDays(7); // 8-day range: startDate + 7 days

        log.info("Fetching forecast for: {} from {} to {}", city, startDate, endDate);

        try {
            Map<String, ForecastWeatherResponse.ForecastEntry> dailyForecast =
                    weatherApiService.getDailyForecast(city);

            String response = weatherApiService.formatForecast(city, dailyForecast, startDate, endDate);
            return DialogflowResponse.of(response);

        } catch (Exception e) {
            log.error("Error fetching forecast for city: {}", city, e);
            return DialogflowResponse.of(
                    String.format("Sorry, I couldn't retrieve the forecast for '%s'. " +
                            "Please try again later.", city));
        }
    }

    /**
     * Extracts the city from Dialogflow parameters.
     * Dialogflow ES uses "@sys.geo-city" entity, mapped to "geo-city" in parameters.
     */
    private String extractCity(Map<String, Object> parameters) {
        if (parameters == null) return null;

        // Dialogflow may send geo-city as a string or as nested object
        Object cityObj = parameters.get("geo-city");
        if (cityObj == null) {
            cityObj = parameters.get("city"); // fallback key
        }

        if (cityObj instanceof String) {
            return ((String) cityObj).trim();
        } else if (cityObj instanceof Map) {
            // Sometimes Dialogflow wraps it: {"geo-city": "London"}
            Object inner = ((Map<?, ?>) cityObj).get("geo-city");
            return inner != null ? inner.toString().trim() : null;
        }

        return cityObj != null ? cityObj.toString().trim() : null;
    }

    /**
     * Extracts the date from Dialogflow parameters.
     * Dialogflow sends dates in ISO 8601 format: "2024-01-20T12:00:00+05:00"
     * Falls back to today's date if not provided or unparseable.
     */
    private LocalDate extractDate(Map<String, Object> parameters) {
        if (parameters == null) return LocalDate.now();

        Object dateObj = parameters.get("date");
        if (dateObj == null || dateObj.toString().isBlank()) {
            log.debug("No date parameter provided, defaulting to today");
            return LocalDate.now();
        }

        String dateStr = dateObj.toString().trim();
        log.debug("Raw date from Dialogflow: {}", dateStr);

        // Dialogflow sends ISO 8601 datetime strings
        // Try ISO date-time first (with timezone offset), then plain date
        try {
            // Handle: "2024-01-20T00:00:00+05:00" → extract date part only
            if (dateStr.contains("T")) {
                dateStr = dateStr.substring(0, 10); // "2024-01-20"
            }
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date '{}', defaulting to today", dateStr);
            return LocalDate.now();
        }
    }
}
