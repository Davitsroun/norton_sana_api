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
import com.leang.authservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/products")
    public ResponseEntity<ApiResponseWithPagination<ProductViewResponse>> getProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice
    ) {
        Page<Product> products = productRepository.searchProducts(
                null,
                null,
                categoryId,
                minPrice,
                maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<ProductViewResponse> viewPage = products.map(this::toProductView);
        ApiResponseWithPagination<ProductViewResponse> response = ApiResponseWithPagination.itemsAndPaginationResponse(
                viewPage.getContent(),
                page,
                size,
                (int) viewPage.getTotalElements()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable("id") UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        List<ProductViewResponse> relateProduct = Collections.emptyList();
        if (product.getCategory() != null) {
            relateProduct = productRepository
                    .findTop5ByCategoryCategoryIdAndProductIdNotOrderByCreatedAtDesc(
                            product.getCategory().getCategoryId(),
                            id
                    )
                    .stream()
                    .map(this::toProductView)
                    .toList();
        }
        ProductDetailResponse detail = new ProductDetailResponse(
                toProductView(product),
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

    private ProductViewResponse toProductView(Product product) {
        double avgRating = product.getReviews().stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0.0);
        int reviewCount = product.getReviews() == null ? 0 : product.getReviews().size();
        return new ProductViewResponse(
                product.getProductId(),
                product.getBrand() == null ? null : product.getBrand().getBrandId(),
                product.getName(),
                null,
                product.getPrice(),
                null,
                product.getImageUrl(),
                product.getImageUrl2(),
                product.getImageUrl3(),
                product.getImageUrl4(),
                avgRating,
                reviewCount,
                product.getCategory() == null ? null : slugify(product.getCategory().getCategoryName()),
                product.getDescription(),
                null
        );
    }

    private CategoryViewResponse toCategoryView(Category category) {
        return new CategoryViewResponse(
                category.getCategoryId(),
                slugify(category.getCategoryName()),
                category.getCategoryName()
        );
    }

    private String slugify(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "-");
    }
}
