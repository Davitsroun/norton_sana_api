package com.leang.authservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CheckTransactionRequest(
        @NotBlank
        String md5,
        UUID orderId
) {
}
