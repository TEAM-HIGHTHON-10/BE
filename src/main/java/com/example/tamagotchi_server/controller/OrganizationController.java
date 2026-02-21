package com.example.tamagotchi_server.controller;

import com.example.tamagotchi_server.service.GitHubApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final GitHubApiService gitHubApiService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getOrganizations(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(gitHubApiService.getUserOrganizations(userId));
    }

    @GetMapping("/{org}/repos")
    public ResponseEntity<List<Map<String, Object>>> getOrgRepos(
            Authentication auth,
            @PathVariable String org) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(gitHubApiService.getOrgRepos(userId, org));
    }
}
