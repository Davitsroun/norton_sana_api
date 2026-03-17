package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.ReviewCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.model.entity.Review;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController extends BaseResponse {

    private final ReviewService reviewService;
    private final ProductRepository productRepository;

    @Operation(summary = "Create review")
    @PostMapping
    public ResponseEntity<ApiResponse<Review>> create(@RequestBody ReviewCreateRequest dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Review review = Review.builder()
                .userId(dto.getUserId())
                .product(product)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .createdAt(Instant.now())
                .build();

        return responseEntity(true, "Review created successfully.", HttpStatus.CREATED, reviewService.create(review));
    }

    @Operation(summary = "Get review by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Review retrieved successfully.", HttpStatus.OK, reviewService.getById(id));
    }

    @Operation(summary = "Get all reviews")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<Review>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<Review> response = reviewService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update review")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> update(@PathVariable UUID id, @RequestBody Review review) {
        return responseEntity(true, "Review updated successfully.", HttpStatus.OK, reviewService.update(id, review));
    }

    @Operation(summary = "Delete review")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        reviewService.delete(id);
        return responseEntity(true, "Review deleted successfully.", HttpStatus.OK);
    }
}

