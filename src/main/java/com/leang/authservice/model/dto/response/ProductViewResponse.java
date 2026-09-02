package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductViewResponse(
        UUID id,
        UUID brandId,
        String name,
        Integer stockQuantity,
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
        String badge,
        BigDecimal costPrice,
        Integer batchCount,
        LocalDate nearestExpiryDate,
        Integer expiredBatchCount,
        Integer expiringSoonQuantity,
        String freshnessLabel
) {
    public ProductViewResponse(
            UUID id,
            UUID brandId,
            String name,
            Integer stockQuantity,
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
            String badge,
            BigDecimal costPrice
    ) {
        this(id, brandId, name, stockQuantity, price, originalPrice, image, imageUrl2, imageUrl3, imageUrl4,
                rating, reviews, category, description, badge, costPrice,
                null, null, null, null, null);
    }
}
