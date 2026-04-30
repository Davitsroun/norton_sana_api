package com.leang.authservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCreateRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID productId,
        @Schema(example = "5")
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,
        @Schema(example = "Great for sensitive skin.")
        String comment
) {
}
