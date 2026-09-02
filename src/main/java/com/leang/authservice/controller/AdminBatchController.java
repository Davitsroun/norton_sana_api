package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.ProductBatchRequest;
import com.leang.authservice.model.dto.request.ProductBatchWriteOffRequest;
import com.leang.authservice.model.dto.response.AdminBatchResponse;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.service.ProductBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/admin/batches")
@RequiredArgsConstructor
@Tag(name = "AdminBatch")
@SecurityRequirement(name = "bearerAuth")
public class AdminBatchController extends BaseResponse {

    private final ProductBatchService productBatchService;

    @Operation(summary = "List all batches across products (paginated + filters)")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<AdminBatchResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "expiringWithinDays", required = false) Integer expiringWithinDays,
            @RequestParam(name = "sort", defaultValue = "expiryDateAsc") String sort
    ) {
        return ResponseEntity.ok(productBatchService.searchAdmin(
                page, size, search, productId, status, expiringWithinDays, sort
        ));
    }

    @Operation(summary = "Batch detail with product snapshot and stock breakdown")
    @GetMapping("/{batchId}")
    public ResponseEntity<ApiResponse<AdminBatchResponse>> getById(@PathVariable UUID batchId) {
        return responseEntity(true, "Batch retrieved successfully.", HttpStatus.OK,
                productBatchService.getAdminById(batchId));
    }

    @Operation(summary = "Create batch (productId required in body)")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminBatchResponse>> create(@Valid @RequestBody ProductBatchRequest request) {
        return responseEntity(true, "Batch created successfully.", HttpStatus.CREATED,
                productBatchService.createGlobal(request));
    }

    @Operation(summary = "Update batch by id")
    @PutMapping("/{batchId}")
    public ResponseEntity<ApiResponse<AdminBatchResponse>> update(
            @PathVariable UUID batchId,
            @Valid @RequestBody ProductBatchRequest request
    ) {
        return responseEntity(true, "Batch updated successfully.", HttpStatus.OK,
                productBatchService.updateGlobal(batchId, request));
    }

    @Operation(summary = "Write off batch — sets quantity=0, status=WRITTEN_OFF, stores reason")
    @PostMapping("/{batchId}/write-off")
    public ResponseEntity<ApiResponse<AdminBatchResponse>> writeOff(
            @PathVariable UUID batchId,
            @Valid @RequestBody ProductBatchWriteOffRequest request
    ) {
        return responseEntity(true, "Batch written off successfully.", HttpStatus.OK,
                productBatchService.writeOffGlobal(batchId, request.reason()));
    }
}
