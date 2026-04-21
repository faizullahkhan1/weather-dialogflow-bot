package com.weatherbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Maps the forecast response from OpenWeatherMap /forecast endpoint.
 * Returns data every 3 hours, up to 5 days (40 entries).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastWeatherResponse {

    @JsonProperty("city")
    private City city;

    @JsonProperty("list")
    private List<ForecastEntry> list;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class City {
        @JsonProperty("name")
        private String name;

        @JsonProperty("country")
        private String country;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastEntry {

        @JsonProperty("dt")
        private long dt; // Unix timestamp

        @JsonProperty("dt_txt")
        private String dtTxt; // Human-readable datetime string e.g. "2024-01-20 12:00:00"

        @JsonProperty("weather")
        private List<WeatherDesc> weather;

        @JsonProperty("main")
        private MainWeather main;

        @JsonProperty("wind")
        private Wind wind;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class WeatherDesc {
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

            @JsonProperty("humidity")
            private int humidity;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Wind {
            @JsonProperty("speed")
            private double speed;
        }
    }
}
