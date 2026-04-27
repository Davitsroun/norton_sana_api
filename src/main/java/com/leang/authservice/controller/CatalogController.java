package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.CategoryViewResponse;
import com.leang.authservice.model.dto.response.ProductViewResponse;
import com.leang.authservice.model.entity.Category;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.CategoryRepository;
import com.leang.authservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/products")
    public Page<ProductViewResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        String namePattern = (search == null || search.isBlank()) ? null : "%" + search.toLowerCase(Locale.ROOT) + "%";
        Page<Product> products = productRepository.searchProducts(
                namePattern,
                brandId,
                categoryId,
                minPrice,
                maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return products.map(this::toProductView);
    }

    @GetMapping("/products/{id}")
    public ProductViewResponse getProductById(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return toProductView(product);
    }

    @GetMapping("/categories")
    public List<CategoryViewResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryView)
                .toList();
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
                product.getName(),
                product.getPrice(),
                null,
                product.getImageUrl(),
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
