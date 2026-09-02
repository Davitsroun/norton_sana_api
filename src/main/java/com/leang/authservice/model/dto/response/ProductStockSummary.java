package com.leang.authservice.model.dto.response;

import java.time.LocalDate;

public record ProductStockSummary(
        int stockQuantity,
        int batchCount,
        LocalDate nearestExpiryDate,
        int expiredBatchCount,
        int expiringSoonQuantity
) {
}
