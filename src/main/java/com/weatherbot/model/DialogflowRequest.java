package com.weatherbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Represents the incoming WebhookRequest from Dialogflow ES.
 * Dialogflow sends this JSON body when fulfillment is triggered.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DialogflowRequest {

    @JsonProperty("responseId")
    private String responseId;

    @JsonProperty("session")
    private String session;

    @JsonProperty("queryResult")
    private QueryResult queryResult;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryResult {

        @JsonProperty("queryText")
        private String queryText;

        @JsonProperty("action")
        private String action;

        @JsonProperty("parameters")
        private Map<String, Object> parameters;

        @JsonProperty("intent")
        private Intent intent;

        @JsonProperty("languageCode")
        private String languageCode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Intent {

        @JsonProperty("name")
        private String name;

        @JsonProperty("displayName")
        private String displayName;
    }
}
