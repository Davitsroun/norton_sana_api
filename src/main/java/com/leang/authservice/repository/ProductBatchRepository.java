package com.leang.authservice.repository;

import com.leang.authservice.enums.BatchStatus;
import com.leang.authservice.model.entity.ProductBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {

    List<ProductBatch> findAllByProduct_ProductIdOrderByExpiryDateAscReceivedDateAsc(UUID productId);

    Optional<ProductBatch> findByIdAndProduct_ProductId(UUID id, UUID productId);

    boolean existsByProduct_ProductIdAndBatchCodeIgnoreCase(UUID productId, String batchCode);

    @Query("""
            SELECT b FROM ProductBatch b
            JOIN FETCH b.product p
            LEFT JOIN FETCH p.brand
            LEFT JOIN FETCH p.category
            WHERE b.id = :id
            """)
    Optional<ProductBatch> findDetailById(@Param("id") UUID id);

    @Query(
            value = """
                    SELECT b FROM ProductBatch b
                    JOIN b.product p
                    WHERE (:productId IS NULL OR p.productId = :productId)
                      AND (:status IS NULL OR b.status = :status)
                      AND (
                           :searchPattern IS NULL
                           OR LOWER(p.name) LIKE :searchPattern
                           OR LOWER(b.batchCode) LIKE :searchPattern
                      )
                      AND (:expiringUntil IS NULL OR (
                           b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
                           AND b.expiryDate >= :today
                           AND b.expiryDate <= :expiringUntil
                           AND b.quantity > 0
                      ))
                    """,
            countQuery = """
                    SELECT COUNT(b) FROM ProductBatch b
                    JOIN b.product p
                    WHERE (:productId IS NULL OR p.productId = :productId)
                      AND (:status IS NULL OR b.status = :status)
                      AND (
                           :searchPattern IS NULL
                           OR LOWER(p.name) LIKE :searchPattern
                           OR LOWER(b.batchCode) LIKE :searchPattern
                      )
                      AND (:expiringUntil IS NULL OR (
                           b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
                           AND b.expiryDate >= :today
                           AND b.expiryDate <= :expiringUntil
                           AND b.quantity > 0
                      ))
                    """
    )
    Page<ProductBatch> searchAdminBatches(
            @Param("productId") UUID productId,
            @Param("status") BatchStatus status,
            @Param("searchPattern") String searchPattern,
            @Param("today") LocalDate today,
            @Param("expiringUntil") LocalDate expiringUntil,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM ProductBatch b
            WHERE b.product.productId = :productId
            AND b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
            AND b.expiryDate >= :today
            AND b.quantity > 0
            ORDER BY b.expiryDate ASC, b.receivedDate ASC, b.createdAt ASC
            """)
    List<ProductBatch> findSellableForUpdate(@Param("productId") UUID productId, @Param("today") LocalDate today);

    @Query("""
            SELECT COALESCE(SUM(b.quantity), 0) FROM ProductBatch b
            WHERE b.product.productId = :productId
            AND b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
            AND b.expiryDate >= :today
            AND b.quantity > 0
            """)
    int sumSellableQuantity(@Param("productId") UUID productId, @Param("today") LocalDate today);

    long countByProduct_ProductId(UUID productId);

    long countByProduct_ProductIdAndStatus(UUID productId, BatchStatus status);

    @Query("""
            SELECT COALESCE(SUM(b.quantity), 0) FROM ProductBatch b
            WHERE b.product.productId = :productId
            AND b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
            AND b.expiryDate >= :today
            AND b.expiryDate <= :until
            AND b.quantity > 0
            """)
    int sumExpiringSoonQuantity(
            @Param("productId") UUID productId,
            @Param("today") LocalDate today,
            @Param("until") LocalDate until
    );

    @Query("""
            SELECT MIN(b.expiryDate) FROM ProductBatch b
            WHERE b.product.productId = :productId
            AND b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
            AND b.expiryDate >= :today
            AND b.quantity > 0
            """)
    LocalDate findNearestExpiryDate(@Param("productId") UUID productId, @Param("today") LocalDate today);

    @Query("""
            SELECT b FROM ProductBatch b
            WHERE b.status = com.leang.authservice.enums.BatchStatus.EXPIRED
            AND b.quantity > 0
            ORDER BY b.expiryDate ASC
            """)
    List<ProductBatch> findExpiredWithStock();

    @Query("""
            SELECT b FROM ProductBatch b
            WHERE b.status = com.leang.authservice.enums.BatchStatus.ACTIVE
            AND b.expiryDate >= :today
            AND b.expiryDate <= :until
            AND b.quantity > 0
            ORDER BY b.expiryDate ASC
            """)
    List<ProductBatch> findExpiringSoon(@Param("today") LocalDate today, @Param("until") LocalDate until);
}
