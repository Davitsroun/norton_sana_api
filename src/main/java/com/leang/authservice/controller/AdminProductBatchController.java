package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.ProductBatchRequest;
import com.leang.authservice.model.dto.request.ProductBatchWriteOffRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.ProductBatchResponse;
import com.leang.authservice.service.ProductBatchService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products/{productId}/batches")
@RequiredArgsConstructor
@Tag(name = "AdminProductBatch")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductBatchController extends BaseResponse {

    private final ProductBatchService productBatchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductBatchResponse>>> list(@PathVariable UUID productId) {
        return responseEntity(true, "Batches retrieved successfully.", HttpStatus.OK, productBatchService.list(productId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductBatchResponse>> create(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductBatchRequest request
    ) {
        return responseEntity(true, "Batch created successfully.", HttpStatus.CREATED, productBatchService.create(productId, request));
    }

    @PutMapping("/{batchId}")
    public ResponseEntity<ApiResponse<ProductBatchResponse>> update(
            @PathVariable UUID productId,
            @PathVariable UUID batchId,
            @Valid @RequestBody ProductBatchRequest request
    ) {
        return responseEntity(true, "Batch updated successfully.", HttpStatus.OK, productBatchService.update(productId, batchId, request));
    }

    @DeleteMapping("/{batchId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @PathVariable UUID batchId
    ) {
        productBatchService.delete(productId, batchId);
        return responseEntity(true, "Batch deleted successfully.", HttpStatus.OK);
    }

    @PostMapping("/{batchId}/write-off")
    public ResponseEntity<ApiResponse<ProductBatchResponse>> writeOff(
            @PathVariable UUID productId,
            @PathVariable UUID batchId,
            @Valid @RequestBody ProductBatchWriteOffRequest request
    ) {
        return responseEntity(true, "Batch written off successfully.", HttpStatus.OK,
                productBatchService.writeOff(productId, batchId, request.reason()));
    }
}
