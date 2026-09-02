package com.leang.authservice.model.dto.response;

import com.leang.authservice.enums.BatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProductBatchResponse(
        UUID id,
        UUID productId,
        String batchCode,
        LocalDate expiryDate,
        LocalDate receivedDate,
        int quantity,
        Integer initialQuantity,
        BigDecimal costPrice,
        BatchStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
