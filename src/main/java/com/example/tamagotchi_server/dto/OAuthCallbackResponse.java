package com.example.tamagotchi_server.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthCallbackResponse {

    private String token;
    private String username;
    private int xp;
    private String level;
}
