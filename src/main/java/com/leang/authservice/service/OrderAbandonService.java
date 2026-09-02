package com.leang.authservice.service;

import com.leang.authservice.exception.ConflictException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.util.FulfillmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderAbandonService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BatchInventoryService batchInventoryService;
    private final OrderViewMapper orderViewMapper;

    @Transactional
    public OrderViewResponse abandonForUser(UUID orderId, UUID userId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return abandon(order);
    }

    @Transactional
    public OrderViewResponse abandonForGuest(UUID orderId, UUID sessionId) {
        Order order = orderRepository.findByOrderIdAndSessionIdAndUserIdIsNull(orderId, sessionId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return abandon(order);
    }

    private OrderViewResponse abandon(Order order) {
        try {
            FulfillmentValidator.assertActiveCartStatus(order.getStatus());
        } catch (ConflictException ex) {
            throw new ConflictException("Only pending or processing orders can be abandoned", Map.of("status", order.getStatus()));
        }

        UUID orderId = order.getOrderId();
        List<OrderItem> lines = orderItemRepository.findAllByOrder_OrderId(orderId);
        for (OrderItem line : lines) {
            batchInventoryService.restoreAllForOrderItem(line.getOrderItemId());
        }

        orderItemRepository.deleteAllByOrder_OrderId(orderId);
        order.setTotalPrice(BigDecimal.ZERO);
        order.setStatus("cancelled");
        Order saved = orderRepository.save(order);
        return orderViewMapper.toView(saved);
    }
}
