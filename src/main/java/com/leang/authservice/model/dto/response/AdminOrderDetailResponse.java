package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailResponse(
        UUID id,
        UUID userId,
        String customerName,
        String customerEmail,
        String contactNumber,
        String fulfillment,
        String deliveryAddress,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String province,
        String district,
        String commune,
        String placeId,
        String deliveryInstructions,
        String pickupNotes,
        String paymentMethod,
        String paymentStatus,
        Instant placedAt,
        BigDecimal totalAmount,
        String currency,
        String status,
        String avatarUrl,
        String trackingNumber,
        String guestEmail,
        List<OrderLineViewResponse> items
) {
}
