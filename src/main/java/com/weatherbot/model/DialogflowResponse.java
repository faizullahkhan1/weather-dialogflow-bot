package com.weatherbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the WebhookResponse sent back to Dialogflow ES.
 * The fulfillmentText is what the bot will say to the user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogflowResponse {

    @JsonProperty("fulfillmentText")
    private String fulfillmentText;

    /**
     * Static factory method for creating a simple text response.
     */
    public static DialogflowResponse of(String message) {
        return DialogflowResponse.builder()
                .fulfillmentText(message)
                .build();
    }
}
