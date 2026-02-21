package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.dto.StatusResponse;
import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.enums.Level;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final UserRepository userRepository;

    public StatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        int xpToNext = user.getLevel().isMax()
                ? 0
                : Level.XP_PER_LEVEL - user.getCurrentLevelXp();

        return StatusResponse.builder()
                .username(user.getUsername())
                .level(user.getLevel().name())
                .currentLevelXp(user.getCurrentLevelXp())
                .xpToNextLevel(xpToNext)
                .eggCount(user.getEggCount())
                .totalXp(user.getXp())
                .build();
    }
}
