package com.leang.authservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReviewUpdateRequest(
        @Schema(example = "4")
        @Min(1)
        @Max(5)
        Integer rating,
        String comment
) {
}
