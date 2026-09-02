package com.leang.authservice.model.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Create/update batch. For global {@code POST /admin/batches}, {@code productId} is required.
 * For product-scoped {@code POST /admin/products/{productId}/batches}, path productId wins.
 */
public record ProductBatchRequest(
        UUID productId,
        String batchCode,
        LocalDate expiryDate,
        LocalDate receivedDate,
        Integer quantity,
        BigDecimal costPrice,
        /** Allow creating a batch with expiry in the past (admin override). */
        Boolean allowPastExpiry
) {
}
