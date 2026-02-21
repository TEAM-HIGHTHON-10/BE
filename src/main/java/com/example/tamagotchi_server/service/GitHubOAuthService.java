package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.dto.GitHubTokenResponse;
import com.example.tamagotchi_server.dto.GitHubUserResponse;
import com.example.tamagotchi_server.dto.OAuthCallbackResponse;
import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.repository.UserRepository;
import com.example.tamagotchi_server.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final WebClient webClient;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    private static final String SCOPES = "repo read:org user:follow";

    /**
     * Build the GitHub OAuth authorization URL.
     */
    public String buildAuthorizationUrl() {
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=" + SCOPES.replace(" ", "%20");
    }

    /**
     * Exchange authorization code for access token, fetch user info,
     * create or update the user, and return a JWT.
     */
    public OAuthCallbackResponse handleCallback(String code) {
        // Exchange code for access token
        GitHubTokenResponse tokenResponse = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenRequest(clientId, clientSecret, code, redirectUri))
                .retrieve()
                .bodyToMono(GitHubTokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Failed to obtain access token from GitHub");
        }

        // Fetch GitHub user profile
        GitHubUserResponse userResponse = webClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                .retrieve()
                .bodyToMono(GitHubUserResponse.class)
                .block();

        if (userResponse == null) {
            throw new RuntimeException("Failed to fetch GitHub user info");
        }

        // Fetch initial follower count for new users
        long followerCount = userResponse.getFollowers() != null ? userResponse.getFollowers() : 0;

        // Create or update user
        User user = userRepository.findByGithubId(userResponse.getId())
                .map(existing -> {
                    existing.setUsername(userResponse.getLogin());
                    existing.setAccessToken(tokenResponse.getAccessToken());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .githubId(userResponse.getId())
                        .username(userResponse.getLogin())
                        .accessToken(tokenResponse.getAccessToken())
                        .followerCount(followerCount)
                        .build()));

        String jwt = jwtProvider.generateToken(user.getId(), user.getUsername());

        return OAuthCallbackResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .xp(user.getXp())
                .level(user.getLevel().getDisplayName())
                .build();
    }

    /** Inner record used as the token exchange request body. */
    private record TokenRequest(String client_id, String client_secret, String code, String redirect_uri) {}
}
