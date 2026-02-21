package com.example.tamagotchi_server.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameResultRequest {

    private String result;  // "SUCCESS" or "FAIL"
}
