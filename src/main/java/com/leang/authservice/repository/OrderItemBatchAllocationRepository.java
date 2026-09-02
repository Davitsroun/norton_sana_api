package com.leang.authservice.repository;

import com.leang.authservice.model.entity.OrderItemBatchAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemBatchAllocationRepository extends JpaRepository<OrderItemBatchAllocation, UUID> {

    List<OrderItemBatchAllocation> findAllByOrderItem_OrderItemIdOrderByCreatedAtAsc(UUID orderItemId);

    List<OrderItemBatchAllocation> findAllByOrderItem_OrderItemIdOrderByCreatedAtDesc(UUID orderItemId);

    void deleteAllByOrderItem_OrderItemId(UUID orderItemId);
}
