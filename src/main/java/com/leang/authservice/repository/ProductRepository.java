package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findTop5ByCategoryCategoryIdAndProductIdNotOrderByCreatedAtDesc(UUID categoryId, UUID productId);

    @Query("""
            SELECT p FROM Product p
            WHERE (:namePattern IS NULL OR LOWER(p.name) LIKE :namePattern)
              AND (:brandId IS NULL OR p.brand.brandId = :brandId)
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> searchProducts(
            @Param("namePattern") String namePattern,
            @Param("brandId") UUID brandId,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}


