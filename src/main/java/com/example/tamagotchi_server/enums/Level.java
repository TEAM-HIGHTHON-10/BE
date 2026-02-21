package com.example.tamagotchi_server.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Level {

    NEWBIE("입문"),
    JUNIOR("주니어"),
    MIDDLE("미들"),
    SENIOR("시니어");

    private final String displayName;

    /** Every level requires 100 XP to advance */
    public static final int XP_PER_LEVEL = 100;

    /**
     * Returns the next level, or null if already SENIOR (cap).
     */
    public Level nextLevel() {
        Level[] levels = values();
        int idx = this.ordinal();
        if (idx < levels.length - 1) {
            return levels[idx + 1];
        }
        return null; // SENIOR is the cap
    }

    /**
     * Check if this is the maximum level.
     */
    public boolean isMax() {
        return this == SENIOR;
    }
}
