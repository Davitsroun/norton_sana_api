package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Review;
import com.leang.authservice.repository.ReviewRepository;
import com.leang.authservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public Review create(Review review) {
        review.setCreatedAt(Instant.now());
        return reviewRepository.save(review);
    }

    @Override
    public Review update(UUID id, Review review) {
        Review existing = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        existing.setRating(review.getRating());
        existing.setComment(review.getComment());
        existing.setProduct(review.getProduct());
        existing.setUserId(review.getUserId());
        return reviewRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Review existing = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        reviewRepository.delete(existing);
    }

    @Override
    public Review getById(UUID id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found"));
    }

    @Override
    public ApiResponseWithPagination<Review> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                reviewPage.getContent(),
                page,
                size,
                (int) reviewPage.getTotalElements()
        );
    }
}

