package com.leang.authservice.repository;

import com.leang.authservice.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    Optional<OrderItem> findByOrderItemIdAndOrder_UserId(UUID orderItemId, UUID userId);

    Optional<OrderItem> findByOrderItemIdAndOrder_SessionIdAndOrder_UserIdIsNull(UUID orderItemId, UUID sessionId);

    List<OrderItem> findAllByOrder_OrderId(UUID orderId);

    void deleteAllByOrder_OrderId(UUID orderId);

    @Query(
            value = "SELECT SUM(price) FROM order_item WHERE order_id = :orderId",
            nativeQuery = true
    )
    BigDecimal getTotalPriceByOrderId(@Param("orderId") UUID orderId);

    @Query(value = """
            SELECT COALESCE(SUM(COALESCE(oi.unit_cost, 0) * oi.quantity), 0)
            FROM order_item oi
            INNER JOIN orders o ON o.order_id = oi.order_id
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
            """, nativeQuery = true)
    BigDecimal sumPaidCost();

    @Query(value = """
            SELECT COALESCE(SUM(COALESCE(oi.unit_cost, 0) * oi.quantity), 0)
            FROM order_item oi
            INNER JOIN orders o ON o.order_id = oi.order_id
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
              AND o.created_at >= :from AND o.created_at < :to
            """, nativeQuery = true)
    BigDecimal sumCostBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT DATE_TRUNC('month', o.created_at AT TIME ZONE 'UTC') AS period_start,
                   COALESCE(SUM(oi.price), 0) AS revenue,
                   COALESCE(SUM(COALESCE(oi.unit_cost, 0) * oi.quantity), 0) AS cost
            FROM order_item oi
            INNER JOIN orders o ON o.order_id = oi.order_id
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
              AND o.created_at >= :from AND o.created_at < :to
            GROUP BY DATE_TRUNC('month', o.created_at AT TIME ZONE 'UTC')
            ORDER BY period_start
            """, nativeQuery = true)
    List<Object[]> profitByMonth(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT DATE_TRUNC('year', o.created_at AT TIME ZONE 'UTC') AS period_start,
                   COALESCE(SUM(oi.price), 0) AS revenue,
                   COALESCE(SUM(COALESCE(oi.unit_cost, 0) * oi.quantity), 0) AS cost
            FROM order_item oi
            INNER JOIN orders o ON o.order_id = oi.order_id
            WHERE LOWER(TRIM(o.status)) IN ('paid', 'shipped', 'completed')
              AND o.created_at >= :from AND o.created_at < :to
            GROUP BY DATE_TRUNC('year', o.created_at AT TIME ZONE 'UTC')
            ORDER BY period_start
            """, nativeQuery = true)
    List<Object[]> profitByYear(@Param("from") Instant from, @Param("to") Instant to);
}

