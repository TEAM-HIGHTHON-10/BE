package com.example.tamagotchi_server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookRegisterRequest {

    private String owner;
    private String repo;
}
