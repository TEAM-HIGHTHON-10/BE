package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.dto.XpEventMessage;
import com.example.tamagotchi_server.entity.GithubEvent;
import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.exception.GitHubRateLimitException;
import com.example.tamagotchi_server.repository.GithubEventRepository;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job that syncs follower counts from GitHub every 5 minutes.
 * Per-user isolation: each user's follower delta is tracked independently.
 *
 * New game logic: follower increase awards eggs, not XP directly.
 * Each new follower = 1 follower quest = +10 eggs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FollowerSyncService {

    private final UserRepository userRepository;
    private final GitHubApiService gitHubApiService;
    private final GithubEventRepository githubEventRepository;
    private final XpService xpService;
    private final WebSocketSessionService webSocketSessionService;

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void syncAllFollowers() {
        log.info("Starting follower sync for all users");

        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                syncFollowersForUser(user);
            } catch (GitHubRateLimitException e) {
                log.warn("Skipping follower sync for user {} (rate-limited until {})",
                        user.getUsername(), e.getResetAt());
            } catch (Exception e) {
                log.error("Failed to sync followers for user {}: {}",
                        user.getUsername(), e.getMessage());
            }
        }

        log.info("Follower sync completed");
    }

    @Transactional
    public void syncFollowersForUser(User user) {
        if (user.isRateLimited()) {
            log.debug("Skipping rate-limited user: {}", user.getUsername());
            return;
        }

        long currentCount = gitHubApiService.getFollowerCount(user);
        long storedCount = user.getFollowerCount();

        if (currentCount > storedCount) {
            long newFollowers = currentCount - storedCount;
            log.info("User {} gained {} new follower(s) ({} -> {})",
                    user.getUsername(), newFollowers, storedCount, currentCount);

            // Dedup: keyed by the new follower count to prevent double-counting
            String uniqueId = "follower_to:" + currentCount;
            if (githubEventRepository.existsByUserIdAndEventTypeAndEventUniqueId(
                    user.getId(), "follower", uniqueId)) {
                log.debug("Follower event already recorded for user {} at count {}",
                        user.getId(), currentCount);
                return;
            }

            githubEventRepository.save(GithubEvent.builder()
                    .userId(user.getId())
                    .repo("github/followers")
                    .eventType("follower")
                    .eventUniqueId(uniqueId)
                    .build());

            // Award eggs: each new follower = 1 quest = +10 eggs
            int eggsPerQuest = xpService.getEggsPerQuest();
            int totalEggs = (int) (newFollowers * eggsPerQuest);
            User updated = xpService.addEggs(user.getId(), totalEggs);

            // Update stored follower count
            updated.setFollowerCount(currentCount);
            userRepository.save(updated);

            // Notify via WebSocket — only to this user
            XpEventMessage message = XpEventMessage.builder()
                    .type("QUEST_COMPLETED")
                    .questType("FOLLOWER")
                    .eggsEarned(totalEggs)
                    .totalEggs(updated.getEggCount())
                    .build();

            webSocketSessionService.sendMessage(user.getId(), message);

        } else if (currentCount < storedCount) {
            // Follower count decreased (unfollows) — update stored value, no penalty
            log.info("User {} lost followers ({} -> {}), updating count",
                    user.getUsername(), storedCount, currentCount);
            user.setFollowerCount(currentCount);
            userRepository.save(user);
        }
    }
}
