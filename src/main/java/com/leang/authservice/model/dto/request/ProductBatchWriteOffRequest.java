package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProductBatchWriteOffRequest(
        @NotBlank
        String reason
) {
}
