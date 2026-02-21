package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.security.WebhookSignatureValidator;
import com.example.tamagotchi_server.service.WebhookProcessingService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives webhook events from GitHub.
 * Validates the signature header before processing.
 */
@RestController
@RequestMapping("/github/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookSignatureValidator signatureValidator;
    private final WebhookProcessingService webhookProcessingService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] payload) {

        // Verify webhook signature
        if (!signatureValidator.isValid(signature, payload)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            Map<String, Object> payloadMap = objectMapper.readValue(
                    payload, new TypeReference<Map<String, Object>>() {});

            webhookProcessingService.processEvent(event, payloadMap);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }
    }
}
