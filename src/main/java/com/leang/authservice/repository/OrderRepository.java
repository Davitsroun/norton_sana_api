package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("select o.orderId from Order o where o.userId = :userId and o.status = :status")
    UUID getOrderIByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByOrderIdAndUserId(UUID orderId, UUID userId);

    /** Unpaid / in-progress orders (cart + checkout before payment). */
    @Query("""
            SELECT o FROM Order o
            WHERE o.userId = :userId
            AND LOWER(TRIM(o.status)) IN ('pending', 'processing')
            ORDER BY o.createdAt DESC
            """)
    List<Order> findActiveOrdersForUser(@Param("userId") UUID userId);

    /** Paid or completed orders for purchase history. */
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
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'completed')
            """)
    BigDecimal sumPaidRevenue();

    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR LOWER(TRIM(o.status)) = LOWER(TRIM(:status)))
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findAdminOrders(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.createdAt >= :from AND o.createdAt < :to
            """)
    long countOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT DATE_TRUNC('month', created_at) AS month_start,
                   COALESCE(SUM(total_price), 0) AS revenue
            FROM orders
            WHERE LOWER(TRIM(status)) IN ('paid', 'completed')
              AND created_at >= :from
              AND created_at < :to
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY month_start
            """, nativeQuery = true)
    List<Object[]> revenueByMonth(@Param("from") Instant from, @Param("to") Instant to);
}

