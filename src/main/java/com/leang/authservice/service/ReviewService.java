package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.ReviewCreateRequest;
import com.leang.authservice.model.dto.request.ReviewUpdateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.ReviewViewResponse;

import java.util.List;
import java.util.UUID;

public interface    ReviewService {

    ReviewViewResponse createReview(UUID userId, ReviewCreateRequest request);

    ReviewViewResponse updateReview(UUID userId, UUID reviewId, ReviewUpdateRequest request);

    void deleteReview(UUID userId, UUID reviewId);

    ReviewViewResponse getReviewViewById(UUID id);

    ApiResponseWithPagination<ReviewViewResponse> listByProduct(UUID productId, int page, int size);

    /**
     * Newest reviews for a product (for product detail embed). Does not re-check product existence.
     */
    List<ReviewViewResponse> listRecentReviewsForProduct(UUID productId, int limit);
}
