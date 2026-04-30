package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.FavoriteBrandCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.FavoriteBrand;
import com.leang.authservice.service.FavoriteBrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorite-brands")
@RequiredArgsConstructor
@Tag(name = "Favorite Brand")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteBrandController extends BaseResponse {

    private final FavoriteBrandService favoriteBrandService;

    @Operation(summary = "Create favorite brand")
    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteBrand>> create(@RequestBody FavoriteBrandCreateRequest dto) {
        return responseEntity(true, "Favorite brand created successfully.", HttpStatus.CREATED, favoriteBrandService.create(dto));
    }

    @Operation(summary = "Get favorite brand by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FavoriteBrand>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Favorite brand retrieved successfully.", HttpStatus.OK, favoriteBrandService.getById(id));
    }

    @Operation(summary = "Get all favorite brands (current user)")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<FavoriteBrand>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<FavoriteBrand> response = favoriteBrandService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Delete favorite brand")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        favoriteBrandService.delete(id);
        return responseEntity(true, "Favorite brand deleted successfully.", HttpStatus.OK);
    }
}

