package com.example.tamagotchi_server.repository;

import com.example.tamagotchi_server.entity.UserRepoGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepoGrantRepository extends JpaRepository<UserRepoGrant, Long> {

    /** Find all users who granted a specific repo — used to route webhook events */
    List<UserRepoGrant> findByRepoFullName(String repoFullName);

    /** Check if a specific user already granted a repo */
    Optional<UserRepoGrant> findByUserIdAndRepoFullName(Long userId, String repoFullName);

    /** List all repos granted by a user */
    List<UserRepoGrant> findByUserId(Long userId);
}
