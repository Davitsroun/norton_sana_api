package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.PaymentProfileRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.PaymentProfileResponse;
import com.leang.authservice.service.PaymentProfileService;
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
@RequestMapping("/api/v1/payment-profiles")
@RequiredArgsConstructor
@Tag(name = "Payment Profile")
@SecurityRequirement(name = "bearerAuth")
public class PaymentProfileController extends BaseResponse {

    private final PaymentProfileService paymentProfileService;

    @Operation(summary = "Create payment profile for current user")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentProfileResponse>> create(@Valid @RequestBody PaymentProfileRequest request) {
        return responseEntity(true, "Payment profile created successfully.", HttpStatus.CREATED, paymentProfileService.create(request));
    }

    @Operation(summary = "Update payment profile for current user")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentProfileResponse>> update(@PathVariable UUID id, @Valid @RequestBody PaymentProfileRequest request) {
        return responseEntity(true, "Payment profile updated successfully.", HttpStatus.OK, paymentProfileService.update(id, request));
    }

    @Operation(summary = "Get one payment profile for current user")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentProfileResponse>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Payment profile retrieved successfully.", HttpStatus.OK, paymentProfileService.getById(id));
    }

    @Operation(summary = "List payment profiles for current user")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<PaymentProfileResponse>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<PaymentProfileResponse> response = paymentProfileService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Delete payment profile for current user")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        paymentProfileService.delete(id);
        return responseEntity(true, "Payment profile deleted successfully.", HttpStatus.OK);
    }
}
