package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.CategoryViewResponse;
import com.leang.authservice.model.dto.response.ProductDetailResponse;
import com.leang.authservice.model.dto.response.ProductViewResponse;
import com.leang.authservice.model.entity.Category;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.CategoryRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.ProductViewMapper;
import com.leang.authservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController extends BaseResponse {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewService reviewService;
    private final ProductViewMapper productViewMapper;

    @Value("${catalog.hide-out-of-stock:true}")
    private boolean hideOutOfStock;

    @GetMapping("/products")
    public ResponseEntity<ApiResponseWithPagination<ProductViewResponse>> getProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "inStockOnly", required = false) Boolean inStockOnly
    ) {
        boolean filterInStock = inStockOnly != null ? inStockOnly : hideOutOfStock;
        String namePattern = null;
        if (name != null && !name.isBlank()) {
            namePattern = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
        }
        Page<Product> products = productRepository.searchProducts(
                namePattern,
                null,
                categoryId,
                minPrice,
                maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ProductViewResponse> items = products.getContent().stream()
                .map(productViewMapper::toPublicView)
                .filter(p -> !filterInStock || (p.stockQuantity() != null && p.stockQuantity() > 0))
                .toList();
        ApiResponseWithPagination<ProductViewResponse> response = ApiResponseWithPagination.itemsAndPaginationResponse(
                items,
                page,
                size,
                (int) products.getTotalElements()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable("id") UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        ProductViewResponse view = productViewMapper.toPublicView(product);
        List<ProductViewResponse> relateProduct = Collections.emptyList();
        if (product.getCategory() != null) {
            relateProduct = productRepository
                    .findTop5ByCategoryCategoryIdAndProductIdNotOrderByCreatedAtDesc(
                            product.getCategory().getCategoryId(),
                            id
                    )
                    .stream()
                    .map(productViewMapper::toPublicView)
                    .filter(p -> p.stockQuantity() != null && p.stockQuantity() > 0)
                    .toList();
        }
        ProductDetailResponse detail = new ProductDetailResponse(
                view,
                relateProduct,
                reviewService.listRecentReviewsForProduct(id, 100)
        );
        return responseEntity(true, "Product retrieved successfully.", HttpStatus.OK, detail);
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryViewResponse>>> getCategories() {
        List<CategoryViewResponse> categories = categoryRepository.findAll().stream()
                .map(this::toCategoryView)
                .toList();
        return responseEntity(true, "Categories retrieved successfully.", HttpStatus.OK, categories);
    }

    private CategoryViewResponse toCategoryView(Category category) {
        return new CategoryViewResponse(
                category.getCategoryId(),
                productViewMapper.slugify(category.getCategoryName()),
                category.getCategoryName()
        );
    }
}
