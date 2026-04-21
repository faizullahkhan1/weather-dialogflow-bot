package com.weatherbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Maps the current weather response from OpenWeatherMap /weather endpoint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentWeatherResponse {

    @JsonProperty("name")
    private String cityName;

    @JsonProperty("weather")
    private List<WeatherDescription> weather;

    @JsonProperty("main")
    private MainWeather main;

    @JsonProperty("wind")
    private Wind wind;

    @JsonProperty("sys")
    private Sys sys;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherDescription {
        @JsonProperty("main")
        private String main;

        @JsonProperty("description")
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MainWeather {
        @JsonProperty("temp")
        private double temp;

        @JsonProperty("feels_like")
        private double feelsLike;

        @JsonProperty("humidity")
        private int humidity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        @JsonProperty("speed")
        private double speed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        @JsonProperty("country")
        private String country;
    }
}
