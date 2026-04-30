package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.ReviewCreateRequest;
import com.leang.authservice.model.dto.request.ReviewUpdateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.ReviewViewResponse;
import com.leang.authservice.service.CurrentUserService;
import com.leang.authservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review")
public class ReviewController extends BaseResponse {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "List reviews for a product")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<ReviewViewResponse>> listByProduct(
            @RequestParam UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<ReviewViewResponse> response = reviewService.listByProduct(productId, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Get review by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewViewResponse>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Review retrieved successfully.", HttpStatus.OK, reviewService.getReviewViewById(id));
    }

    @Operation(summary = "Create a review")
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ReviewViewResponse>> create(@Valid @RequestBody ReviewCreateRequest request) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        return responseEntity(true, "Review created successfully.", HttpStatus.CREATED, reviewService.createReview(userId, request));
    }

    @Operation(summary = "Update your review")
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ReviewViewResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        return responseEntity(true, "Review updated successfully.", HttpStatus.OK, reviewService.updateReview(userId, id, request));
    }

    @Operation(summary = "Delete your review")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        reviewService.deleteReview(userId, id);
        return responseEntity(true, "Review deleted successfully.", HttpStatus.OK);
    }
}
