package com.example.tamagotchi_server.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusResponse {

    private String username;
    private String level;
    private int currentLevelXp;
    private int xpToNextLevel;
    private int eggCount;
    private int totalXp;
}
