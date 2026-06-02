package com.leang.authservice.repository;

import com.leang.authservice.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByKeycloakId(String keycloakId);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}
