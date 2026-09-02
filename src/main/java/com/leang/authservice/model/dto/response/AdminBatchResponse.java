package com.leang.authservice.model.dto.response;

import com.leang.authservice.enums.BatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin batch row/detail — includes product snapshot and stock breakdown.
 * {@code soldQuantity} is computed: max(0, initialQuantity - quantity).
 */
public record AdminBatchResponse(
        UUID id,
        UUID productId,
        String productName,
        String productImage,
        String productBrand,
        String productCategory,
        String batchCode,
        LocalDate expiryDate,
        LocalDate receivedDate,
        int initialQuantity,
        int quantity,
        int soldQuantity,
        BigDecimal costPrice,
        BatchStatus status,
        String writeOffReason,
        Instant createdAt,
        Instant updatedAt
) {
}
