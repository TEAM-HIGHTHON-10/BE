package com.example.tamagotchi_server.exception;

import lombok.Getter;

import java.time.Instant;

/**
 * Thrown when a GitHub API call is rejected due to rate limiting (HTTP 403).
 * Carries the reset timestamp so callers can schedule retries.
 */
@Getter
public class GitHubRateLimitException extends RuntimeException {

    private final Instant resetAt;

    public GitHubRateLimitException(Instant resetAt) {
        super("GitHub API rate limit exceeded. Resets at: " + resetAt);
        this.resetAt = resetAt;
    }
}
