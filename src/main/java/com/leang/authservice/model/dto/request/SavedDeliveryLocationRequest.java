package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavedDeliveryLocationRequest(
        @NotBlank
        @Size(min = 1, max = 40)
        String label,
        String deliveryAddress,
        String formattedAddress,
        @NotNull
        Double latitude,
        @NotNull
        Double longitude,
        String province,
        String district,
        String commune,
        String placeId,
        String deliveryInstructions,
        Boolean isDefault
) {
}
