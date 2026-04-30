package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.BrandCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Brand;
import com.leang.authservice.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brand")
@SecurityRequirement(name = "bearerAuth")
public class BrandController extends BaseResponse {

    private final BrandService brandService;

    @Operation(summary = "Create brand")
    @PostMapping
    public ResponseEntity<ApiResponse<Brand>> create(@RequestBody BrandCreateRequest dto) {
        Brand brand = Brand.builder()
                .brandName(dto.getBrandName())
                .country(dto.getCountry())
                .build();
        return responseEntity(true, "Brand created successfully.", HttpStatus.CREATED, brandService.create(brand));
    }

    @Operation(summary = "Get brand by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Brand retrieved successfully.", HttpStatus.OK, brandService.getById(id));
    }

    @Operation(summary = "Get all brands")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<Brand>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<Brand> response = brandService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update brand")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> update(@PathVariable UUID id, @RequestBody Brand brand) {
        return responseEntity(true, "Brand updated successfully.", HttpStatus.OK, brandService.update(id, brand));
    }

    @Operation(summary = "Delete brand")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        brandService.delete(id);
        return responseEntity(true, "Brand deleted successfully.", HttpStatus.OK);
    }
}

