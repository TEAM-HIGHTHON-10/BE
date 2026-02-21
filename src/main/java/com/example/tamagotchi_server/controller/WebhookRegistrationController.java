package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.dto.WebhookRegisterRequest;
import com.example.tamagotchi_server.entity.UserRepoGrant;
import com.example.tamagotchi_server.repository.UserRepoGrantRepository;
import com.example.tamagotchi_server.service.GitHubApiService;
import lombok.RequiredArgsConstructor;
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
public class WebhookRegistrationController {

    private final GitHubApiService gitHubApiService;
    private final UserRepoGrantRepository userRepoGrantRepository;

    /**
     * Register a webhook on a GitHub repository AND persist a UserRepoGrant
     * so incoming events from this repo are routed to this user.
     *
     * Idempotent: if the user already granted this repo, the grant is updated
     * with the new webhook ID (GitHub may return a new one).
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerWebhook(
            Authentication auth,
            @RequestBody WebhookRegisterRequest request) {

        Long userId = (Long) auth.getPrincipal();
        String repoFullName = request.getOwner() + "/" + request.getRepo();

        // Register webhook on GitHub
        Map<String, Object> result = gitHubApiService.registerWebhook(
                userId, request.getOwner(), request.getRepo());

        // Extract webhook ID from GitHub response
        Long webhookId = result.get("id") != null
                ? ((Number) result.get("id")).longValue()
                : null;

        // Upsert repo grant: create or update existing
        UserRepoGrant grant = userRepoGrantRepository
                .findByUserIdAndRepoFullName(userId, repoFullName)
                .orElse(UserRepoGrant.builder()
                        .userId(userId)
                        .repoFullName(repoFullName)
                        .build());
        grant.setWebhookId(webhookId);
        userRepoGrantRepository.save(grant);

        return ResponseEntity.ok(result);
    }
}
