package com.leang.authservice.model.dto.response;

import com.leang.authservice.enums.BatchStatus;

import java.time.LocalDate;
import java.util.UUID;

public record StockAlertItemResponse(
        String alertType,
        UUID productId,
        String productName,
        String productImageUrl,
        UUID batchId,
        String batchCode,
        LocalDate expiryDate,
        int quantity,
        BatchStatus status
) {
}
