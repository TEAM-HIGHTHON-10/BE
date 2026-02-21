package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.exception.GitHubRateLimitException;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Centralized wrapper around WebClient for all GitHub API calls.
 * Handles:
 * - Rate limit detection (X-RateLimit-Remaining / X-RateLimit-Reset)
 * - Per-user rate limit tracking (stored in DB)
 * - Exponential backoff retry (up to 3 attempts)
 * - Skipping calls for users currently rate-limited
 */
@Component
@Slf4j
public class GitHubApiClient {

    private final WebClient webClient;
    private final UserRepository userRepository;

    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    public GitHubApiClient(WebClient webClient, UserRepository userRepository) {
        this.webClient = webClient;
        this.userRepository = userRepository;
    }

    /**
     * Execute a GET request against the GitHub API with rate limit handling.
     * Throws GitHubRateLimitException if the user is currently rate-limited
     * or if GitHub returns a 403 with exhausted rate limit.
     */
    public <T> T get(User user, String uri, ParameterizedTypeReference<T> responseType) {
        checkUserRateLimit(user);
        return executeWithRetry(() ->
                webClient.get()
                        .uri(uri)
                        .header("Authorization", "Bearer " + user.getAccessToken())
                        .exchangeToMono(response -> handleResponse(response, user, responseType))
                        .block()
        );
    }

    /**
     * Execute a GET request with URI variables.
     */
    public <T> T get(User user, String uri, ParameterizedTypeReference<T> responseType, Object... uriVars) {
        checkUserRateLimit(user);
        return executeWithRetry(() ->
                webClient.get()
                        .uri(uri, uriVars)
                        .header("Authorization", "Bearer " + user.getAccessToken())
                        .exchangeToMono(response -> handleResponse(response, user, responseType))
                        .block()
        );
    }

    /**
     * Execute a POST request against the GitHub API with rate limit handling.
     */
    public <T> T post(User user, String uri, Object body, ParameterizedTypeReference<T> responseType,
                      Object... uriVars) {
        checkUserRateLimit(user);
        return executeWithRetry(() ->
                webClient.post()
                        .uri(uri, uriVars)
                        .header("Authorization", "Bearer " + user.getAccessToken())
                        .bodyValue(body)
                        .exchangeToMono(response -> handleResponse(response, user, responseType))
                        .block()
        );
    }

    /**
     * Process the response: check for rate limit headers and handle 403.
     */
    private <T> Mono<T> handleResponse(ClientResponse response, User user,
                                       ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = response.headers().asHttpHeaders();
        updateRateLimitInfo(user, headers);

        HttpStatusCode status = response.statusCode();

        // Rate limit exceeded → 403 with X-RateLimit-Remaining: 0
        if (status.value() == 403) {
            String remaining = headers.getFirst("X-RateLimit-Remaining");
            if ("0".equals(remaining)) {
                Instant resetAt = parseResetHeader(headers);
                markUserRateLimited(user, resetAt);
                return Mono.error(new GitHubRateLimitException(resetAt));
            }
        }

        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(
                            new RuntimeException("GitHub API error " + status.value() + ": " + body)));
        }

        return response.bodyToMono(responseType);
    }

    /**
     * Log rate limit info from response headers for monitoring.
     */
    private void updateRateLimitInfo(User user, HttpHeaders headers) {
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        String limit = headers.getFirst("X-RateLimit-Limit");
        if (remaining != null && limit != null) {
            int rem = Integer.parseInt(remaining);
            log.debug("GitHub rate limit for user {}: {}/{} remaining", user.getUsername(), remaining, limit);

            // Clear rate limit flag if we're no longer limited
            if (rem > 0 && user.getRateLimitResetAt() != null) {
                user.setRateLimitResetAt(null);
                userRepository.save(user);
            }
        }
    }

    private Instant parseResetHeader(HttpHeaders headers) {
        String reset = headers.getFirst("X-RateLimit-Reset");
        if (reset != null) {
            return Instant.ofEpochSecond(Long.parseLong(reset));
        }
        // Fallback: reset in 60 seconds
        return Instant.now().plusSeconds(60);
    }

    private void markUserRateLimited(User user, Instant resetAt) {
        log.warn("User {} is rate-limited by GitHub until {}", user.getUsername(), resetAt);
        user.setRateLimitResetAt(resetAt);
        userRepository.save(user);
    }

    /**
     * Skip API calls entirely if the user is already known to be rate-limited.
     */
    private void checkUserRateLimit(User user) {
        if (user.isRateLimited()) {
            throw new GitHubRateLimitException(user.getRateLimitResetAt());
        }
    }

    /**
     * Retry with exponential backoff. Only retries on GitHubRateLimitException.
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> action) {
        Duration backoff = INITIAL_BACKOFF;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (GitHubRateLimitException e) {
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                log.info("Rate limited, retrying in {} (attempt {}/{})", backoff, attempt, MAX_RETRIES);
                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                backoff = backoff.multipliedBy(2);
            }
        }
        throw new RuntimeException("Unreachable");
    }
}
