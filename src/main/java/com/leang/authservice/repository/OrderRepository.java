package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("select o.orderId from Order o where o.userId = :userId and o.status = :status")
    UUID getOrderIByUserIdAndStatus(UUID userId, String status);

}

