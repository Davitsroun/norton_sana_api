package com.leang.authservice.model.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SavedDeliveryLocationResponse(
        UUID id,
        String label,
        String deliveryAddress,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String province,
        String district,
        String commune,
        String placeId,
        String deliveryInstructions,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
}
