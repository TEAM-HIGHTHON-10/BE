package com.example.tamagotchi_server.repository;

import com.example.tamagotchi_server.entity.GithubEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GithubEventRepository extends JpaRepository<GithubEvent, Long> {

    /** Per-user duplicate check: same user + eventType + uniqueId means already processed */
    boolean existsByUserIdAndEventTypeAndEventUniqueId(Long userId, String eventType, String eventUniqueId);

    /** Latest events for a user, ordered by creation time descending */
    List<GithubEvent> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
