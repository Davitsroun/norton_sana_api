package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductViewResponse(
        UUID id,
        UUID brandId,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        String image,
        String imageUrl2,
        String imageUrl3,
        String imageUrl4,
        Double rating,
        Integer reviews,
        String category,
        String description,
        String badge
) {
}
