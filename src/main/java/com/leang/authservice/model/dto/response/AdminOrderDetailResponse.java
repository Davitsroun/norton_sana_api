package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin order detail: list fields plus line items ({@link OrderLineViewResponse}).
 */
public record AdminOrderDetailResponse(
        UUID id,
        String customerName,
        String customerEmail,
        String deliveryAddress,
        Instant placedAt,
        BigDecimal totalAmount,
        String currency,
        String status,
        String avatarUrl,
        String contactNumber,
        String paymentMethod,
        String fulfillment,
        String trackingNumber,
        String guestEmail,
        List<OrderLineViewResponse> items
) {
}
