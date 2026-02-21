package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.dto.GameResultRequest;
import com.example.tamagotchi_server.dto.GameResultResponse;
import com.example.tamagotchi_server.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * POST /api/game/result — 게임 결과 전송.
     * SUCCESS → 10 eggs 지급 + WebSocket QUEST_COMPLETED/GAME
     * FAIL → 지급 없음
     */
    @PostMapping("/result")
    public ResponseEntity<GameResultResponse> submitGameResult(
            Authentication auth,
            @RequestBody GameResultRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(gameService.processGameResult(userId, request));
    }
}
