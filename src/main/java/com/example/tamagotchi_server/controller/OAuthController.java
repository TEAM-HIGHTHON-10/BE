package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.dto.OAuthCallbackResponse;
import com.example.tamagotchi_server.service.GitHubOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/oauth/github")
@RequiredArgsConstructor
public class OAuthController {

    private final GitHubOAuthService oAuthService;

    /**
     * Redirects the user to GitHub's OAuth authorization page.
     */
    @GetMapping("/login")
    public RedirectView login() {
        String url = oAuthService.buildAuthorizationUrl();
        return new RedirectView(url);
    }

    /**
     * Handles the OAuth callback from GitHub.
     * Exchanges the code for a token, creates/updates the user, and returns a JWT.
     */
    @GetMapping("/callback")
    public ResponseEntity<OAuthCallbackResponse> callback(@RequestParam String code) {
        OAuthCallbackResponse response = oAuthService.handleCallback(code);
        return ResponseEntity.ok(response);
    }
}
