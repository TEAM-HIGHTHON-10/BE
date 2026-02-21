package com.example.tamagotchi_server.service;

import com.example.tamagotchi_server.dto.XpEventMessage;
import com.example.tamagotchi_server.entity.GithubEvent;
import com.example.tamagotchi_server.entity.User;
import com.example.tamagotchi_server.entity.UserRepoGrant;
import com.example.tamagotchi_server.repository.GithubEventRepository;
import com.example.tamagotchi_server.repository.UserRepoGrantRepository;
import com.example.tamagotchi_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Processes incoming GitHub webhook events with multi-user isolation.
 *
 * New game logic: events do NOT award XP directly.
 * Instead: GitHub Event → Quest Completed → +10 Eggs → User clicks Feed → XP
 *
 * Each quest completion:
 * 1. Dedup check via GithubEvent (userId, eventType, eventUniqueId)
 * 2. Award +10 eggs to the user
 * 3. Push QUEST_COMPLETED WebSocket message to that user only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final GithubEventRepository githubEventRepository;
    private final UserRepoGrantRepository userRepoGrantRepository;
    private final UserRepository userRepository;
    private final XpService xpService;
    private final WebSocketSessionService webSocketSessionService;

    @Transactional
    public void processEvent(String eventType, Map<String, Object> payload) {
        // Extract repo full name — this is the multi-user isolation key
        Map<String, Object> repository = extractMap(payload, "repository");
        if (repository == null) {
            log.warn("No repository found in webhook payload, skipping");
            return;
        }
        String repoFullName = (String) repository.get("full_name");
        if (repoFullName == null) {
            log.warn("repository.full_name is null, skipping");
            return;
        }

        // Find all users who granted this repo
        List<UserRepoGrant> grants = userRepoGrantRepository.findByRepoFullName(repoFullName);
        if (grants.isEmpty()) {
            log.info("No users granted repo {}, ignoring event", repoFullName);
            return;
        }

        // Process the event for each granted user independently
        for (UserRepoGrant grant : grants) {
            User user = userRepository.findById(grant.getUserId()).orElse(null);
            if (user == null) {
                log.warn("Grant references non-existent userId {}, skipping", grant.getUserId());
                continue;
            }

            switch (eventType) {
                case "push" -> processPush(user, repoFullName, payload);
                case "pull_request" -> processPullRequest(user, repoFullName, payload);
                case "issues" -> processIssue(user, repoFullName, payload);
                default -> log.debug("Unhandled event type: {}", eventType);
            }
        }
    }

    /** Push event → Commit quest. Uses head_commit.id for dedup. */
    private void processPush(User user, String repo, Map<String, Object> payload) {
        Map<String, Object> headCommit = extractMap(payload, "head_commit");
        if (headCommit == null) return;

        String sha = (String) headCommit.get("id");
        if (sha == null) return;

        if (saveEventIfNew(user.getId(), repo, "push", sha)) {
            awardEggsAndNotify(user.getId(), "COMMIT");
        }
    }

    /** PR opened event → PR quest. Uses PR number for dedup. */
    private void processPullRequest(User user, String repo, Map<String, Object> payload) {
        String action = (String) payload.get("action");
        if (!"opened".equals(action)) return;

        Map<String, Object> pr = extractMap(payload, "pull_request");
        if (pr == null) return;

        String prNumber = String.valueOf(((Number) pr.get("number")).longValue());
        if (saveEventIfNew(user.getId(), repo, "pull_request", prNumber)) {
            awardEggsAndNotify(user.getId(), "PR");
        }
    }

    /** Issue opened event → Issue quest. Uses issue number for dedup. */
    private void processIssue(User user, String repo, Map<String, Object> payload) {
        String action = (String) payload.get("action");
        if (!"opened".equals(action)) return;

        Map<String, Object> issue = extractMap(payload, "issue");
        if (issue == null) return;

        String issueNumber = String.valueOf(((Number) issue.get("number")).longValue());
        if (saveEventIfNew(user.getId(), repo, "issues", issueNumber)) {
            awardEggsAndNotify(user.getId(), "ISSUE");
        }
    }

    /** Per-user dedup: (userId, eventType, eventUniqueId) must be unique */
    private boolean saveEventIfNew(Long userId, String repo, String eventType, String uniqueId) {
        if (githubEventRepository.existsByUserIdAndEventTypeAndEventUniqueId(userId, eventType, uniqueId)) {
            log.debug("Duplicate event for user {}: {} / {}", userId, eventType, uniqueId);
            return false;
        }

        githubEventRepository.save(GithubEvent.builder()
                .userId(userId)
                .repo(repo)
                .eventType(eventType)
                .eventUniqueId(uniqueId)
                .build());
        return true;
    }

    /**
     * Award eggs (not XP) for quest completion and push WebSocket notification.
     * Eggs are only converted to XP when the user explicitly calls the Feed API.
     */
    private void awardEggsAndNotify(Long userId, String questType) {
        int eggs = xpService.getEggsPerQuest();
        User updated = xpService.addEggs(userId, eggs);

        XpEventMessage message = XpEventMessage.builder()
                .type("QUEST_COMPLETED")
                .questType(questType)
                .eggsEarned(eggs)
                .totalEggs(updated.getEggCount())
                .build();

        webSocketSessionService.sendMessage(userId, message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
}
