package com.weatherbot.service;

import com.weatherbot.model.CurrentWeatherResponse;
import com.weatherbot.model.DialogflowRequest;
import com.weatherbot.model.DialogflowResponse;
import com.weatherbot.model.ForecastWeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DialogflowServiceTest {

    @Mock
    private WeatherApiService weatherApiService;

    @InjectMocks
    private DialogflowService dialogflowService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ─── Helper builders ──────────────────────────────────────────────────────

    private DialogflowRequest buildRequest(String action, String intentName,
                                            Map<String, Object> params) {
        DialogflowRequest req = new DialogflowRequest();
        DialogflowRequest.QueryResult qr = new DialogflowRequest.QueryResult();
        qr.setAction(action);
        qr.setParameters(params);
        DialogflowRequest.Intent intent = new DialogflowRequest.Intent();
        intent.setDisplayName(intentName);
        qr.setIntent(intent);
        req.setQueryResult(qr);
        return req;
    }

    private CurrentWeatherResponse mockCurrentWeather(String city) {
        CurrentWeatherResponse weather = new CurrentWeatherResponse();
        weather.setCityName(city);
        CurrentWeatherResponse.WeatherDescription desc = new CurrentWeatherResponse.WeatherDescription();
        desc.setMain("Clear");
        desc.setDescription("clear sky");
        weather.setWeather(List.of(desc));
        CurrentWeatherResponse.MainWeather main = new CurrentWeatherResponse.MainWeather();
        main.setTemp(25.0);
        main.setFeelsLike(24.0);
        main.setHumidity(60);
        weather.setMain(main);
        CurrentWeatherResponse.Wind wind = new CurrentWeatherResponse.Wind();
        wind.setSpeed(3.5);
        weather.setWind(wind);
        CurrentWeatherResponse.Sys sys = new CurrentWeatherResponse.Sys();
        sys.setCountry("PK");
        weather.setSys(sys);
        return weather;
    }

    // ─── Current Weather Tests ────────────────────────────────────────────────

    @Test
    void testCurrentWeatherIntent_returnsFormattedResponse() {
        Map<String, Object> params = new HashMap<>();
        params.put("geo-city", "Karachi");

        when(weatherApiService.getCurrentWeather("Karachi")).thenReturn(mockCurrentWeather("Karachi"));
        when(weatherApiService.formatCurrentWeather(any())).thenReturn("Current weather in Karachi, PK: Clear sky, 25.0°C");

        DialogflowRequest request = buildRequest("weather.current", "Current Weather", params);
        DialogflowResponse response = dialogflowService.handleWebhook(request);

        assertNotNull(response);
        assertTrue(response.getFulfillmentText().contains("Karachi"));
    }

    @Test
    void testCurrentWeatherIntent_noCityProvided_returnsPrompt() {
        Map<String, Object> params = new HashMap<>();

        DialogflowRequest request = buildRequest("weather.current", "Current Weather", params);
        DialogflowResponse response = dialogflowService.handleWebhook(request);

        assertNotNull(response);
        assertTrue(response.getFulfillmentText().toLowerCase().contains("city"));
    }

    // ─── Forecast Tests ───────────────────────────────────────────────────────

    @Test
    void testForecastIntent_withDate_returnsFormattedForecast() {
        Map<String, Object> params = new HashMap<>();
        params.put("geo-city", "London");
        params.put("date", LocalDate.now().toString());

        Map<String, ForecastWeatherResponse.ForecastEntry> dailyMap = new TreeMap<>();
        // Add a mock entry for today
        ForecastWeatherResponse.ForecastEntry entry = new ForecastWeatherResponse.ForecastEntry();
        entry.setDtTxt(LocalDate.now() + " 12:00:00");
        ForecastWeatherResponse.ForecastEntry.WeatherDesc wd =
                new ForecastWeatherResponse.ForecastEntry.WeatherDesc();
        wd.setDescription("cloudy");
        entry.setWeather(List.of(wd));
        ForecastWeatherResponse.ForecastEntry.MainWeather mw =
                new ForecastWeatherResponse.ForecastEntry.MainWeather();
        mw.setTemp(18.0);
        mw.setHumidity(75);
        entry.setMain(mw);
        dailyMap.put(LocalDate.now().toString(), entry);

        when(weatherApiService.getDailyForecast("London")).thenReturn(dailyMap);
        when(weatherApiService.formatForecast(eq("London"), any(), any(), any()))
                .thenReturn("Forecast for London: Cloudy 18.0°C");

        DialogflowRequest request = buildRequest("weather.forecast", "Weather Forecast", params);
        DialogflowResponse response = dialogflowService.handleWebhook(request);

        assertNotNull(response);
        assertNotNull(response.getFulfillmentText());
    }

    @Test
    void testForecastIntent_noDate_defaultsToToday() {
        Map<String, Object> params = new HashMap<>();
        params.put("geo-city", "Dubai");
        // no date param

        when(weatherApiService.getDailyForecast("Dubai")).thenReturn(new TreeMap<>());
        when(weatherApiService.formatForecast(eq("Dubai"), any(), any(), any()))
                .thenReturn("Forecast for Dubai");

        DialogflowRequest request = buildRequest("weather.forecast", "Weather Forecast", params);
        DialogflowResponse response = dialogflowService.handleWebhook(request);

        assertNotNull(response);
        verify(weatherApiService).getDailyForecast("Dubai");
    }

    // ─── Edge Cases ───────────────────────────────────────────────────────────

    @Test
    void testNullRequest_returnsErrorMessage() {
        DialogflowResponse response = dialogflowService.handleWebhook(null);
        assertNotNull(response);
        assertFalse(response.getFulfillmentText().isBlank());
    }

    @Test
    void testUnknownIntent_returnsDefaultMessage() {
        DialogflowRequest request = buildRequest("unknown.action", "Something Else", new HashMap<>());
        DialogflowResponse response = dialogflowService.handleWebhook(request);
        assertNotNull(response);
        assertFalse(response.getFulfillmentText().isBlank());
    }
}
