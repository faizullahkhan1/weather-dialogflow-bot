package com.weatherbot.service;

import com.weatherbot.model.CurrentWeatherResponse;
import com.weatherbot.model.ForecastWeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service responsible for calling the OpenWeatherMap API.
 * Handles both current weather and 5-day forecast requests.
 */
@Slf4j
@Service
public class WeatherApiService {

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public WeatherApiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetches current weather for a given city name.
     * Calls: GET /weather?q={city}&appid={apiKey}&units=metric
     */
    public CurrentWeatherResponse getCurrentWeather(String city) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/weather")
                .queryParam("q", city)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .toUriString();

        log.debug("Fetching current weather for city: {}", city);
        log.debug("Request URL: {}", url.replace(apiKey, "****"));

        return restTemplate.getForObject(url, CurrentWeatherResponse.class);
    }

    /**
     * Fetches 5-day forecast (every 3 hours = 40 entries) for a given city.
     * Calls: GET /forecast?q={city}&appid={apiKey}&units=metric
     */
    public ForecastWeatherResponse getWeatherForecast(String city) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/forecast")
                .queryParam("q", city)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .toUriString();

        log.debug("Fetching forecast for city: {}", city);
        return restTemplate.getForObject(url, ForecastWeatherResponse.class);
    }

    /**
     * Groups 3-hourly forecast entries by date and returns one entry per day.
     * Picks the midday (12:00:00) entry for each date as the representative forecast.
     */
    public Map<String, ForecastWeatherResponse.ForecastEntry> getDailyForecast(String city) {
        ForecastWeatherResponse forecastResponse = getWeatherForecast(city);

        if (forecastResponse == null || forecastResponse.getList() == null) {
            return new TreeMap<>();
        }

        // Group by date (first 10 chars of dt_txt e.g. "2024-01-20")
        Map<String, List<ForecastWeatherResponse.ForecastEntry>> grouped = forecastResponse.getList()
                .stream()
                .collect(Collectors.groupingBy(e -> e.getDtTxt().substring(0, 10)));

        // For each date, pick the entry closest to noon (12:00:00), fallback to first entry
        Map<String, ForecastWeatherResponse.ForecastEntry> dailyMap = new TreeMap<>();
        for (Map.Entry<String, List<ForecastWeatherResponse.ForecastEntry>> entry : grouped.entrySet()) {
            String date = entry.getKey();
            List<ForecastWeatherResponse.ForecastEntry> entries = entry.getValue();

            ForecastWeatherResponse.ForecastEntry midday = entries.stream()
                    .filter(e -> e.getDtTxt().contains("12:00:00"))
                    .findFirst()
                    .orElse(entries.get(0));

            dailyMap.put(date, midday);
        }

        return dailyMap;
    }

    /**
     * Formats a current weather response into a human-readable string for the bot.
     */
    public String formatCurrentWeather(CurrentWeatherResponse weather) {
        if (weather == null) return "Sorry, I could not retrieve weather data.";

        String description = weather.getWeather() != null && !weather.getWeather().isEmpty()
                ? weather.getWeather().get(0).getDescription()
                : "N/A";

        return String.format(
                "🌤 Current weather in %s, %s:\n" +
                "• Condition: %s\n" +
                "• Temperature: %.1f°C (Feels like %.1f°C)\n" +
                "• Humidity: %d%%\n" +
                "• Wind Speed: %.1f m/s",
                weather.getCityName(),
                weather.getSys() != null ? weather.getSys().getCountry() : "",
                capitalize(description),
                weather.getMain().getTemp(),
                weather.getMain().getFeelsLike(),
                weather.getMain().getHumidity(),
                weather.getWind() != null ? weather.getWind().getSpeed() : 0.0
        );
    }

    /**
     * Formats forecast data for a date range into a human-readable string.
     */
    public String formatForecast(String city, Map<String, ForecastWeatherResponse.ForecastEntry> dailyForecast,
                                  LocalDate startDate, LocalDate endDate) {
        if (dailyForecast == null || dailyForecast.isEmpty()) {
            return "Sorry, I could not retrieve forecast data.";
        }

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 Weather forecast for %s from %s to %s:\n\n",
                city,
                startDate.format(displayFormatter),
                endDate.format(displayFormatter)));

        // Filter entries to the requested date range
        LocalDate current = startDate;
        int dayCount = 0;
        while (!current.isAfter(endDate) && dayCount < 8) {
            String dateKey = current.toString(); // "yyyy-MM-dd"
            ForecastWeatherResponse.ForecastEntry entry = dailyForecast.get(dateKey);

            if (entry != null) {
                String desc = entry.getWeather() != null && !entry.getWeather().isEmpty()
                        ? entry.getWeather().get(0).getDescription()
                        : "N/A";

                sb.append(String.format("• %s: %s, %.1f°C, Humidity: %d%%\n",
                        current.format(displayFormatter),
                        capitalize(desc),
                        entry.getMain().getTemp(),
                        entry.getMain().getHumidity()));
            } else {
                sb.append(String.format("• %s: Data not available\n",
                        current.format(displayFormatter)));
            }

            current = current.plusDays(1);
            dayCount++;
        }

        return sb.toString().trim();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
