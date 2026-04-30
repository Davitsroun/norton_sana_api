package com.leang.authservice.model.dto.response;

import java.util.List;

public record ProductDetailResponse(
        ProductViewResponse product,
        List<ProductViewResponse> relateProduct,
        List<ReviewViewResponse> reviewver
) {
}
