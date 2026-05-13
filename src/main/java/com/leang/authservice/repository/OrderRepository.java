package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
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
}

