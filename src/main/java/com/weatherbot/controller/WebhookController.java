package com.weatherbot.controller;

import com.weatherbot.model.DialogflowRequest;
import com.weatherbot.model.DialogflowResponse;
import com.weatherbot.service.DialogflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the webhook endpoint for Dialogflow ES.
 *
 * Endpoint: POST /webhook
 *
 * Dialogflow ES will POST a WebhookRequest JSON body to this URL.
 * This controller passes it to DialogflowService and returns the WebhookResponse.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class WebhookController {

    private final DialogflowService dialogflowService;

    /**
     * Main webhook endpoint.
     * Dialogflow ES calls this URL when fulfillment is enabled for an intent.
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DialogflowResponse> handleWebhook(
            @RequestBody DialogflowRequest request) {

        log.info("=== Incoming Dialogflow Webhook Request ===");
        log.debug("Session: {}", request.getSession());
        log.debug("Intent: {}", request.getQueryResult() != null
                ? request.getQueryResult().getIntent() : "null");

        DialogflowResponse response = dialogflowService.handleWebhook(request);

        log.info("=== Outgoing Webhook Response: {} ===", response.getFulfillmentText());
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint — useful to verify the server is running
     * before configuring in Dialogflow or ngrok.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Weather Bot Webhook is running! ✅");
    }
}
