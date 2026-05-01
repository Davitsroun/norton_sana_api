package com.leang.authservice.repository;

import com.leang.authservice.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    Optional<OrderItem> findByOrderItemIdAndOrder_UserId(UUID orderItemId, UUID userId);

    @Query(
            value = "SELECT SUM(price) FROM order_item WHERE order_id = :orderId",
            nativeQuery = true
    )
    BigDecimal getTotalPriceByOrderId(@Param("orderId") UUID orderId);
}

