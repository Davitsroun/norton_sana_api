package com.leang.authservice.model.dto.request;

import com.leang.authservice.enums.DeliveryOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentProfileRequest(
        @NotNull
        DeliveryOption deliveryOption,
        @NotBlank
        String fullName,
        @NotBlank
        String contactNumber,
        String deliveryAddress
) {
}
