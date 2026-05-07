package com.leang.authservice.model.dto.response;

import java.util.UUID;

public record FavoriteBrandResponse(
        UUID favoriteBrandId,
        UUID brandId,
        String brandName,
        String country,
        UUID productId,
        String productName,
        String imageUrl
) {
}
