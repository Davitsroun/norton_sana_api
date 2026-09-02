package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ProductStockSummary;
import com.leang.authservice.model.dto.response.ProductViewResponse;
import com.leang.authservice.model.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ProductViewMapper {

    private final BatchInventoryService batchInventoryService;

    public ProductViewResponse toPublicView(Product product) {
        int sellable = batchInventoryService.getSellableQuantity(product.getProductId());
        String freshness = sellable > 0 ? "Fresh stock available" : null;
        return baseView(product, sellable, null, freshness);
    }

    public ProductViewResponse toAdminView(Product product) {
        ProductStockSummary summary = batchInventoryService.getStockSummary(product.getProductId());
        return baseView(product, summary.stockQuantity(), summary, null);
    }

    private ProductViewResponse baseView(
            Product product,
            int sellable,
            ProductStockSummary adminSummary,
            String freshnessLabel
    ) {
        double avgRating = product.getReviews() == null ? 0.0 : product.getReviews().stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0.0);
        int reviewCount = product.getReviews() == null ? 0 : product.getReviews().size();
        String category = product.getCategory() == null ? null
                : product.getCategory().getCategoryName();
        return new ProductViewResponse(
                product.getProductId(),
                product.getBrand() == null ? null : product.getBrand().getBrandId(),
                product.getName(),
                sellable,
                product.getPrice(),
                null,
                product.getImageUrl(),
                product.getImageUrl2(),
                product.getImageUrl3(),
                product.getImageUrl4(),
                avgRating,
                reviewCount,
                category,
                product.getDescription(),
                null,
                product.getCostPrice(),
                adminSummary == null ? null : adminSummary.batchCount(),
                adminSummary == null ? null : adminSummary.nearestExpiryDate(),
                adminSummary == null ? null : adminSummary.expiredBatchCount(),
                adminSummary == null ? null : adminSummary.expiringSoonQuantity(),
                freshnessLabel
        );
    }

    public String slugify(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "-");
    }
}
