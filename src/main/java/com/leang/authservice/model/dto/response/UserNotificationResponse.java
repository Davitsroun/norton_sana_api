package com.leang.authservice.model.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserNotificationResponse(
        UUID notificationId,
        UUID userId,
        String type,
        String title,
        String body,
        UUID orderId,
        UUID paymentId,
        boolean read,
        Instant createdAt
) {
}
