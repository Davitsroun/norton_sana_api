package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductViewResponse(
        UUID id,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        String image,
        Double rating,
        Integer reviews,
        String category,
        String description,
        String badge
) {
}
