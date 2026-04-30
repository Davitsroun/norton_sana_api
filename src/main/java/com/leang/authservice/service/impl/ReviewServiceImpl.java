package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ForbiddenException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.ReviewCreateRequest;
import com.leang.authservice.model.dto.request.ReviewUpdateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.ReviewViewResponse;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.model.entity.Review;
import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.repository.ReviewRepository;
import com.leang.authservice.service.CurrentUserService;
import com.leang.authservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ReviewViewResponse createReview(UUID userId, ReviewCreateRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        UserProfile profile = currentUserService.ensureProfile();
        if (!userId.toString().equals(profile.getKeycloakId())) {
            throw new ForbiddenException("User mismatch.");
        }
        Review review = Review.builder()
                .userId(userId)
                .userName(displayName(profile))
                .userImageUrl(currentUserService.resolveProfileImageUrl(profile))
                .product(product)
                .rating(request.rating())
                .comment(request.comment())
                .createdAt(Instant.now())
                .build();
        return toView(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewViewResponse updateReview(UUID userId, UUID reviewId, ReviewUpdateRequest request) {
        if (request.rating() == null && request.comment() == null) {
            throw new BadRequestException("At least one of rating or comment must be provided.");
        }
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!existing.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own reviews.");
        }
        if (request.rating() != null) {
            existing.setRating(request.rating());
        }
        if (request.comment() != null) {
            existing.setComment(request.comment());
        }
        UserProfile profile = currentUserService.ensureProfile();
        existing.setUserName(displayName(profile));
        existing.setUserImageUrl(currentUserService.resolveProfileImageUrl(profile));
        return toView(reviewRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteReview(UUID userId, UUID reviewId) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!existing.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own reviews.");
        }
        reviewRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewViewResponse getReviewViewById(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        return toView(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseWithPagination<ReviewViewResponse> listByProduct(UUID productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found");
        }
        Page<Review> reviewPage = reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(
                productId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                reviewPage.getContent().stream().map(this::toView).toList(),
                page,
                size,
                (int) reviewPage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewViewResponse> listRecentReviewsForProduct(UUID productId, int limit) {
        int cap = Math.min(Math.max(limit, 1), 200);
        Page<Review> reviewPage = reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(
                productId,
                PageRequest.of(0, cap, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return reviewPage.getContent().stream().map(this::toView).toList();
    }

    private ReviewViewResponse toView(Review r) {
        return new ReviewViewResponse(
                r.getReviewId(),
                r.getUserId(),
                r.getUserName(),
                r.getUserImageUrl(),
                r.getProduct().getProductId(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }

    private static String displayName(UserProfile p) {
        if (p.getFirstName() != null && !p.getFirstName().isBlank()) {
            String last = p.getLastName() != null ? p.getLastName().trim() : "";
            String combined = (p.getFirstName().trim() + (last.isEmpty() ? "" : " " + last)).trim();
            if (!combined.isEmpty()) {
                return combined;
            }
        }
        if (p.getUsername() != null && !p.getUsername().isBlank()) {
            return p.getUsername().trim();
        }
        return p.getEmail() != null ? p.getEmail() : "User";
    }
}
