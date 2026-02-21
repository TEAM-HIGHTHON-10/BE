package com.example.tamagotchi_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_events", uniqueConstraints = {
        // Per-user dedup: the same event can be awarded to different users independently,
        // but never twice to the same user.
        @UniqueConstraint(columnNames = {"userId", "eventType", "eventUniqueId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String repo;

    @Column(nullable = false)
    private String eventType;

    /** Unique identifier per event type: commit SHA, PR number, Issue number, or follower marker */
    @Column(nullable = false)
    private String eventUniqueId;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
