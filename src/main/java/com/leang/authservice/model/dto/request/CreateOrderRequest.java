package com.leang.authservice.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull
        List<Item> items,
        String customerName,
        String contactNumber,
        @NotBlank
        String paymentMethod,
        @NotBlank
        @JsonAlias("fulfillmentMethod")
        String fulfillment,
        String deliveryAddress
) {
    public record Item(
            @NotNull
            UUID productId,
            @NotNull
            @Min(1)
            Integer quantity
    ) {
    }
}
