package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.dto.FeedResponse;
import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.enums.Level;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the egg → XP → level-up pipeline.
 *
 * Flow: GitHub Event → Quest Completed → +10 Eggs → User clicks Feed → Eggs become XP → Level Up
 *
 * XP is NEVER awarded directly from events. Events only grant eggs.
 */
@Service
@RequiredArgsConstructor
public class XpService {

    private static final int EGGS_PER_QUEST = 10;

    private final UserRepository userRepository;

    /**
     * Award eggs to a user for completing a quest.
     * Returns the updated user with new egg count.
     */
    @Transactional
    public User addEggs(Long userId, int eggs) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setEggCount(user.getEggCount() + eggs);
        return userRepository.save(user);
    }

    /**
     * Returns the number of eggs awarded per quest completion.
     */
    public int getEggsPerQuest() {
        return EGGS_PER_QUEST;
    }

    /**
     * Feed (밥주기): convert ALL eggs to XP, then process level-ups.
     *
     * Level-up rules:
     * - Each level requires 360 XP (Level.XP_PER_LEVEL)
     * - currentLevelXp resets to 0 on level-up, remainder carries over
     * - SENIOR is the cap — XP continues accumulating but no further level
     */
    @Transactional
    public FeedResponse feed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getEggCount() <= 0) {
            throw new IllegalStateException("No eggs to feed");
        }

        int eggsConsumed = user.getEggCount();
        Level levelBefore = user.getLevel();

        // Convert all eggs to XP
        user.setXp(user.getXp() + eggsConsumed);
        user.setCurrentLevelXp(user.getCurrentLevelXp() + eggsConsumed);
        user.setEggCount(0);

        // Process level-ups: while enough XP and not at cap
        while (user.getCurrentLevelXp() >= Level.XP_PER_LEVEL && !user.getLevel().isMax()) {
            user.setCurrentLevelXp(user.getCurrentLevelXp() - Level.XP_PER_LEVEL);
            user.setLevel(user.getLevel().nextLevel());
        }

        User updated = userRepository.save(user);

        int xpToNext = updated.getLevel().isMax()
                ? 0
                : Level.XP_PER_LEVEL - updated.getCurrentLevelXp();

        return FeedResponse.builder()
                .username(updated.getUsername())
                .level(updated.getLevel().name())
                .currentLevelXp(updated.getCurrentLevelXp())
                .xpToNextLevel(xpToNext)
                .eggCount(updated.getEggCount())
                .totalXp(updated.getXp())
                .eggsConsumed(eggsConsumed)
                .leveledUp(updated.getLevel() != levelBefore)
                .build();
    }
}
