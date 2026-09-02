package com.leang.authservice.repository;

import com.leang.authservice.model.entity.SavedDeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedDeliveryLocationRepository extends JpaRepository<SavedDeliveryLocation, UUID> {

    long countByUserId(UUID userId);

    List<SavedDeliveryLocation> findAllByUserIdOrderByDefaultLocationDescCreatedAtDesc(UUID userId);

    Optional<SavedDeliveryLocation> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SavedDeliveryLocation s SET s.defaultLocation = false WHERE s.userId = :userId AND s.defaultLocation = true")
    int clearDefaultsForUser(@Param("userId") UUID userId);
}
