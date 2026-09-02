package com.leang.authservice.model.dto.response;

import com.leang.authservice.enums.DeliveryOption;

import java.time.Instant;
import java.util.UUID;

public record PaymentProfileResponse(
        UUID paymentProfileId,
        DeliveryOption deliveryOption,
        String fullName,
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
        String pickupNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
