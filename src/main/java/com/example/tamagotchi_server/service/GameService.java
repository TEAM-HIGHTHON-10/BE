package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.dto.GameResultRequest;
import com.example.tamagotchi_server.dto.GameResultResponse;
import com.example.tamagotchi_server.dto.XpEventMessage;
import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final UserRepository userRepository;
    private final XpService xpService;
    private final WebSocketSessionService webSocketSessionService;

    @Transactional
    public GameResultResponse processGameResult(Long userId, GameResultRequest request) {
        String result = request.getResult();

        if (result == null || (!result.equalsIgnoreCase("SUCCESS") && !result.equalsIgnoreCase("FAIL"))) {
            throw new IllegalArgumentException("Invalid result value. Must be SUCCESS or FAIL");
        }

        result = result.toUpperCase();

        if ("SUCCESS".equals(result)) {
            User updated = xpService.addEggs(userId, xpService.getEggsPerQuest());
            int eggsEarned = xpService.getEggsPerQuest();
            int totalEggs = updated.getEggCount();

            // WebSocket 알림
            XpEventMessage wsMessage = XpEventMessage.builder()
                    .type("QUEST_COMPLETED")
                    .questType("GAME")
                    .eggsEarned(eggsEarned)
                    .totalEggs(totalEggs)
                    .build();
            webSocketSessionService.sendMessage(userId, wsMessage);

            log.info("Game SUCCESS for user {}: +{} eggs", userId, eggsEarned);

            return GameResultResponse.builder()
                    .result("SUCCESS")
                    .eggsEarned(eggsEarned)
                    .totalEggs(totalEggs)
                    .build();
        }

        // FAIL
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        log.info("Game FAIL for user {}", userId);

        return GameResultResponse.builder()
                .result("FAIL")
                .eggsEarned(0)
                .totalEggs(user.getEggCount())
                .build();
    }
}
