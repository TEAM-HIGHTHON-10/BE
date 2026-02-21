package com.example.tamagotchi_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResultResponse {

    private String result;

    @JsonProperty("eggs_earned")
    private int eggsEarned;

    @JsonProperty("total_eggs")
    private int totalEggs;
}
