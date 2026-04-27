package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineViewResponse(
        UUID id,
        String productName,
        Integer quantity,
        BigDecimal price,
        String image
) {
}
