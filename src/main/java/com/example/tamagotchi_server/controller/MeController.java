package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.dto.FeedResponse;
import com.example.tamagotchi_server.dto.StatusResponse;
import com.example.tamagotchi_server.service.StatusService;
import com.example.tamagotchi_server.service.XpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final StatusService statusService;
    private final XpService xpService;

    /**
     * GET /api/me/status — returns the authenticated user's tamagotchi status.
     * JWT-protected: only the requesting user's data is returned.
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getMyStatus(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(statusService.getStatus(userId));
    }

    /**
     * POST /api/me/feed — convert ALL accumulated eggs into XP (밥주기).
     *
     * Returns error if eggCount is 0.
     * Processes level-ups if currentLevelXp crosses 360 threshold.
     */
    @PostMapping("/feed")
    public ResponseEntity<FeedResponse> feed(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        FeedResponse response = xpService.feed(userId);
        return ResponseEntity.ok(response);
    }
}
