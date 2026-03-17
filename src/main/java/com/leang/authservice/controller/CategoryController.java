package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.CategoryCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Category;
import com.leang.authservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController extends BaseResponse {

    private final CategoryService categoryService;

    @Operation(summary = "Create category")
    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@RequestBody CategoryCreateRequest dto) {
        Category category = Category.builder()
                .categoryName(dto.getCategoryName())
                .build();
        return responseEntity(true, "Category created successfully.", HttpStatus.CREATED, categoryService.create(category));
    }

    @Operation(summary = "Get category by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Category retrieved successfully.", HttpStatus.OK, categoryService.getById(id));
    }

    @Operation(summary = "Get all categories")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<Category>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<Category> response = categoryService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update category")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable UUID id, @RequestBody Category category) {
        return responseEntity(true, "Category updated successfully.", HttpStatus.OK, categoryService.update(id, category));
    }

    @Operation(summary = "Delete category")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return responseEntity(true, "Category deleted successfully.", HttpStatus.OK);
    }
}

