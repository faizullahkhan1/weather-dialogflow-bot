package com.weatherbot.config;

import com.weatherbot.model.DialogflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Global exception handler for the webhook controller.
 * Catches API errors and returns a user-friendly Dialogflow response
 * rather than an HTTP error that Dialogflow cannot understand.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles OpenWeatherMap API 4xx errors (e.g. 404 city not found, 401 invalid API key).
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<DialogflowResponse> handleHttpClientError(HttpClientErrorException ex) {
        log.error("HTTP Client error calling weather API: {} - {}", ex.getStatusCode(), ex.getMessage());

        String message;
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            message = "I couldn't find that city in my database. " +
                    "Please check the spelling and try again.";
        } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            message = "There's a configuration issue with the weather service. " +
                    "Please contact support.";
        } else {
            message = "I'm having trouble fetching the weather data right now. Please try again.";
        }

        return ResponseEntity.ok(DialogflowResponse.of(message));
    }

    /**
     * Handles network/connection errors to the weather API.
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<DialogflowResponse> handleNetworkError(ResourceAccessException ex) {
        log.error("Network error calling weather API: {}", ex.getMessage());
        return ResponseEntity.ok(DialogflowResponse.of(
                "I'm unable to connect to the weather service right now. Please try again later."));
    }

    /**
     * Catch-all for any unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DialogflowResponse> handleGenericError(Exception ex) {
        log.error("Unexpected error processing webhook request", ex);
        return ResponseEntity.ok(DialogflowResponse.of(
                "An unexpected error occurred. Please try again."));
    }
}
