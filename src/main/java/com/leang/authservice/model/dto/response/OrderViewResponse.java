package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderViewResponse(
        UUID id,
        Instant date,
        List<OrderLineViewResponse> items,
        BigDecimal total,
        String status,
        String trackingNumber,
        String paymentMethod,
        String fulfillment,
        String customerName,
        String contactNumber,
        String deliveryAddress,
        Double latitude,
        Double longitude,
        String province,
        String district,
        String commune,
        String placeId,
        String formattedAddress,
        String deliveryInstructions,
        String pickupNotes
) {
}
