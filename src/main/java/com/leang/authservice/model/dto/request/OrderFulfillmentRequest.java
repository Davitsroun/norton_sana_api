package com.leang.authservice.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.leang.authservice.enums.DeliveryOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderFulfillmentRequest(
        @NotNull
        @JsonAlias("fulfillmentMethod")
        DeliveryOption deliveryOption,
        @NotBlank
        @Size(min = 2, max = 100)
        String fullName,
        @NotBlank
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
        /** Optional: copy location fields from a saved place (owner-scoped). */
        UUID savedLocationId
) {
    public com.leang.authservice.util.FulfillmentValidator.FulfillmentInput toFulfillmentInput() {
        return new com.leang.authservice.util.FulfillmentValidator.FulfillmentInput(
                deliveryOption,
                fullName,
                contactNumber,
                deliveryAddress,
                latitude,
                longitude,
                province,
                district,
                commune,
                placeId,
                formattedAddress,
                deliveryInstructions,
                pickupNotes
        );
    }
}
