package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.ProductCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Brand;
import com.leang.authservice.model.entity.Category;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.BrandRepository;
import com.leang.authservice.repository.CategoryRepository;
import com.leang.authservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product")
@SecurityRequirement(name = "bearerAuth")
public class ProductController extends BaseResponse {

    private final ProductService productService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Operation(summary = "Create product")
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@RequestBody ProductCreateRequest dto) {
        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .imageUrl(dto.getImageUrl())
                .brand(brand)
                .category(category)
                .build();

        return responseEntity(true, "Product created successfully.", HttpStatus.CREATED, productService.create(product));
    }

    @Operation(summary = "Get product by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Product retrieved successfully.", HttpStatus.OK, productService.getById(id));
    }

    @Operation(summary = "Get all products")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<Product>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        ApiResponseWithPagination<Product> response =
                productService.getAll(page, size, name, brandId, categoryId, minPrice, maxPrice);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update product")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(@PathVariable UUID id, @RequestBody Product product) {
        return responseEntity(true, "Product updated successfully.", HttpStatus.OK, productService.update(id, product));
    }

    @Operation(summary = "Delete product")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);
        return responseEntity(true, "Product deleted successfully.", HttpStatus.OK);
    }
}

