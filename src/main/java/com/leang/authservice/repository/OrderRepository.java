package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
            SELECT o.orderId FROM Order o
            WHERE o.userId = :userId
            AND LOWER(TRIM(o.status)) IN ('pending', 'processing')
            ORDER BY o.createdAt DESC
            """)
    List<UUID> findPendingOrderIdsByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT o.orderId FROM Order o
            WHERE o.sessionId = :sessionId
            AND o.userId IS NULL
            AND LOWER(TRIM(o.status)) IN ('pending', 'processing')
            ORDER BY o.createdAt DESC
            """)
    List<UUID> findPendingOrderIdsBySessionId(@Param("sessionId") UUID sessionId);

    /** Legacy exact-status match; prefer findPendingOrderIdByUserId. */
    @Query("select o.orderId from Order o where o.userId = :userId and o.status = :status")
    UUID getOrderIByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByOrderIdAndUserId(UUID orderId, UUID userId);

    Optional<Order> findByOrderIdAndSessionIdAndUserIdIsNull(UUID orderId, UUID sessionId);

    /** Unpaid / in-progress orders (cart + checkout before payment). */
    @EntityGraph(attributePaths = {"items", "items.product", "payment"})
    @Query("""
            SELECT o FROM Order o
            WHERE o.userId = :userId
            AND LOWER(TRIM(o.status)) IN ('pending', 'processing')
            ORDER BY o.createdAt DESC
            """)
    List<Order> findActiveOrdersForUser(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"items", "items.product", "payment"})
    @Query("""
            SELECT o FROM Order o
            WHERE o.sessionId = :sessionId
            AND o.userId IS NULL
            AND LOWER(TRIM(o.status)) IN ('pending', 'processing')
            ORDER BY o.createdAt DESC
            """)
    List<Order> findActiveOrdersForSession(@Param("sessionId") UUID sessionId);

    /** Paid or completed orders for purchase history. */
    @EntityGraph(attributePaths = {"items", "items.product", "payment"})
    @Query("""
            SELECT o FROM Order o
            WHERE o.userId = :userId
            AND LOWER(TRIM(o.status)) IN ('paid', 'completed')
            ORDER BY o.createdAt DESC
            """)
    List<Order> findOrderHistoryForUser(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    @Query("""
            SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
            """)
    BigDecimal sumPaidRevenue();

    @Query("""
            SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
            AND o.createdAt >= :from AND o.createdAt < :to
            """)
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @EntityGraph(attributePaths = {"payment", "items", "items.product"})
    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR LOWER(o.status) = :status)
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findAdminOrders(@Param("status") String status, Pageable pageable);

    @EntityGraph(attributePaths = {"payment", "items", "items.product"})
    Optional<Order> findWithDetailsByOrderId(UUID orderId);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.createdAt >= :from AND o.createdAt < :to
            """)
    long countOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
            AND o.createdAt >= :from AND o.createdAt < :to
            """)
    long countRevenueOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
            """)
    long countRevenueOrders();

    @Query(value = """
            SELECT DATE_TRUNC('month', created_at AT TIME ZONE 'UTC') AS month_start,
                   COALESCE(SUM(total_price), 0) AS revenue
            FROM orders
            WHERE LOWER(TRIM(status)) IN ('paid', 'shipped', 'completed')
              AND created_at >= :from
              AND created_at < :to
            GROUP BY DATE_TRUNC('month', created_at AT TIME ZONE 'UTC')
            ORDER BY month_start
            """, nativeQuery = true)
    List<Object[]> revenueByMonth(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT o FROM Order o
            WHERE o.createdAt >= :since
            ORDER BY o.createdAt DESC
            """)
    List<Order> findOrdersCreatedSince(@Param("since") Instant since);
}
