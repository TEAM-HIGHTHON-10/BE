package com.example.tamagotchi_server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubUserResponse {

    private Long id;
    private String login;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private Long followers;
}
