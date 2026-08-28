package com.leang.authservice.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record CashierPosCheckoutRequest(
        String customerName,
        String contactNumber,
        String guestEmail,
        @NotBlank
        @JsonAlias("fulfillmentMethod")
        String fulfillment,
        @NotBlank String paymentMethod
) {
}
