package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderListItemResponse(
        UUID id,
        String customerName,
        String customerEmail,
        String deliveryAddress,
        Instant placedAt,
        BigDecimal totalAmount,
        String currency,
        String status,
        String avatarUrl
) {
}
