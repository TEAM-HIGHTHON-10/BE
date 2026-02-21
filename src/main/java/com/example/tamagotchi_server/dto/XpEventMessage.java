package com.example.tamagotchi_server.dto;

import lombok.*;

/**
 * WebSocket message sent to the client when a quest is completed.
 * Field name kept as XpEventMessage for wire compatibility, but the content
 * now represents a quest-completion egg reward.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XpEventMessage {

    private String type;        // "QUEST_COMPLETED"
    private String questType;   // "COMMIT", "PR", "ISSUE", "FOLLOWER", "GAME"
    private int eggsEarned;     // eggs gained from this quest
    private int totalEggs;      // total eggs after this quest
}
