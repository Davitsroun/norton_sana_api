package com.leang.authservice.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GuestCheckoutRequest(
        @NotBlank
        @Email
        String guestEmail,
        String customerName,
        String contactNumber,
        @NotBlank
        String paymentMethod,
        @NotBlank
        @JsonAlias("fulfillmentMethod")
        String fulfillment,
        String deliveryAddress
) {
}
