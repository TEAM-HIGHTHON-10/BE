package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.dto.WebhookRegisterRequest;
import com.example.tamagotchi_server.entity.UserRepoGrant;
import com.example.tamagotchi_server.repository.UserRepoGrantRepository;
import com.example.tamagotchi_server.service.GitHubApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookRegistrationController {

    private final GitHubApiService gitHubApiService;
    private final UserRepoGrantRepository userRepoGrantRepository;

    /**
     * Register a webhook on a GitHub repository AND persist a UserRepoGrant
     * so incoming events from this repo are routed to this user.
     *
     * Idempotent: if the webhook already exists on GitHub (422),
     * we still save the UserRepoGrant so events are routed to this user.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerWebhook(
            Authentication auth,
            @RequestBody WebhookRegisterRequest request) {

        Long userId = (Long) auth.getPrincipal();
        String repoFullName = request.getOwner() + "/" + request.getRepo();

        Map<String, Object> result;
        Long webhookId = null;

        try {
            // Register webhook on GitHub
            result = gitHubApiService.registerWebhook(
                    userId, request.getOwner(), request.getRepo());

            // Extract webhook ID from GitHub response
            webhookId = result.get("id") != null
                    ? ((Number) result.get("id")).longValue()
                    : null;
        } catch (Exception e) {
            // GitHub 422 (Hook already exists) — treat as success
            if (e.getMessage() != null && e.getMessage().contains("422")) {
                log.info("Webhook already exists on {}, saving grant for user {}", repoFullName, userId);
                result = Map.of("message", "Webhook already exists, grant registered");
            } else {
                throw e;
            }
        }

        // Always upsert repo grant so events are routed to this user
        UserRepoGrant grant = userRepoGrantRepository
                .findByUserIdAndRepoFullName(userId, repoFullName)
                .orElse(UserRepoGrant.builder()
                        .userId(userId)
                        .repoFullName(repoFullName)
                        .build());
        if (webhookId != null) {
            grant.setWebhookId(webhookId);
        }
        userRepoGrantRepository.save(grant);

        return ResponseEntity.ok(result);
    }
}
