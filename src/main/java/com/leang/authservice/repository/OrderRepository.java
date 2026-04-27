package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("select o.orderId from Order o where o.userId = :userId and o.status = :status")
    UUID getOrderIByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Order> findByOrderIdAndUserId(UUID orderId, UUID userId);
}

