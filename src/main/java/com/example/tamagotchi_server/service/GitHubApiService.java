package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitHubApiService {

    private final UserRepository userRepository;
    private final GitHubApiClient gitHubApiClient;

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    @Value("${github.webhook.callback-url}")
    private String webhookCallbackUrl;

    /**
     * List organizations the authenticated user belongs to.
     */
    public List<Map<String, Object>> getUserOrganizations(Long userId) {
        User user = getUser(userId);
        return gitHubApiClient.get(user,
                "https://api.github.com/user/orgs",
                new ParameterizedTypeReference<>() {});
    }

    /**
     * List repositories for a given organization.
     */
    public List<Map<String, Object>> getOrgRepos(Long userId, String org) {
        User user = getUser(userId);
        return gitHubApiClient.get(user,
                "https://api.github.com/orgs/{org}/repos",
                new ParameterizedTypeReference<>() {},
                org);
    }

    /**
     * Register a webhook on the specified repository for push, pull_request, issues, member events.
     */
    public Map<String, Object> registerWebhook(Long userId, String owner, String repo) {
        User user = getUser(userId);

        Map<String, Object> config = Map.of(
                "url", webhookCallbackUrl,
                "content_type", "json",
                "secret", webhookSecret
        );

        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("push", "pull_request", "issues", "member"),
                "config", config
        );

        return gitHubApiClient.post(user,
                "https://api.github.com/repos/{owner}/{repo}/hooks",
                body,
                new ParameterizedTypeReference<>() {},
                owner, repo);
    }

    /**
     * Fetch the current follower count for a GitHub user.
     * Uses the /users/{username} endpoint which returns a follower count directly,
     * avoiding pagination issues with the /followers list endpoint.
     */
    public long getFollowerCount(User user) {
        Map<String, Object> profile = gitHubApiClient.get(user,
                "https://api.github.com/users/{username}",
                new ParameterizedTypeReference<>() {},
                user.getUsername());

        Number followers = (Number) profile.get("followers");
        return followers != null ? followers.longValue() : 0;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
}
