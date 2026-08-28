package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** Read-only stock row for cashier POS. */
public record CashierStockItemResponse(
        UUID id,
        String name,
        Integer stockQuantity,
        BigDecimal price,
        String image,
        String category,
        boolean lowStock
) {
}
