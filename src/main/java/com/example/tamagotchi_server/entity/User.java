package com.example.tamagotchi_server.entity;

import com.example.tamagotchi_server.enums.Level;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long githubId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 512)
    private String accessToken;

    /** Lifetime total XP accumulated (never resets) */
    @Builder.Default
    @Column(nullable = false)
    private int xp = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level = Level.NEWBIE;

    /** XP progress within the current level (0–359). Resets to 0 on level-up. */
    @Builder.Default
    @Column(nullable = false)
    private int currentLevelXp = 0;

    /** Eggs earned from quest completions. Converted to XP via Feed API. */
    @Builder.Default
    @Column(nullable = false)
    private int eggCount = 0;

    /** Cached follower count for delta-based follower quest tracking */
    @Builder.Default
    @Column(nullable = false)
    private long followerCount = 0;

    /** Timestamp when GitHub API rate limit resets (null = not rate-limited) */
    @Column
    private Instant rateLimitResetAt;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isRateLimited() {
        return rateLimitResetAt != null && Instant.now().isBefore(rateLimitResetAt);
    }
}
