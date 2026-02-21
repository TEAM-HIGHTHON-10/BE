package com.example.tamagotchi_server.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
