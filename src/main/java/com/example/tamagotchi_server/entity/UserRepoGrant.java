package com.example.tamagotchi_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks which repositories a user has explicitly registered for webhook monitoring.
 * Incoming webhook events are only processed for users who have a grant for that repo.
 * This is the core of multi-user isolation.
 */
@Entity
@Table(name = "user_repo_grants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "repoFullName"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRepoGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Full repository name, e.g. "owner/repo" */
    @Column(nullable = false)
    private String repoFullName;

    /** GitHub webhook ID returned after registration (for future unregistration) */
    @Column
    private Long webhookId;

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
