package com.example.tamagotchi_server.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedResponse {

    private String username;
    private String level;
    private int currentLevelXp;
    private int xpToNextLevel;
    private int eggCount;
    private int totalXp;
    private int eggsConsumed;
    private boolean leveledUp;
}
