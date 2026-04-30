package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank
        String name,

        String description,

        @NotNull
        @PositiveOrZero
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity,

        String imageUrl,
        String imageUrl2,
        String imageUrl3,
        String imageUrl4,

        @NotNull
        UUID categoryId,

        UUID brandId
) {
}
