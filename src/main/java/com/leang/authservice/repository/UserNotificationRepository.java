package com.leang.authservice.repository;

import com.leang.authservice.model.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserNotification> findByNotificationIdAndUserId(UUID notificationId, UUID userId);

    boolean existsByUserIdAndOrderIdAndType(UUID userId, UUID orderId, String type);
}
