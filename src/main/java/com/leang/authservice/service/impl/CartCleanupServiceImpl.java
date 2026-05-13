package com.leang.authservice.service.impl;

import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.CartCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartCleanupServiceImpl implements CartCleanupService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void clearOtherActiveOrdersExcept(UUID userId, UUID paidOrderId) {
        List<Order> active = orderRepository.findActiveOrdersForUser(userId);
        for (Order o : active) {
            if (o.getOrderId().equals(paidOrderId)) {
                continue;
            }
            clearLineItemsRestoreStock(o.getOrderId());
            orderRepository.findById(o.getOrderId()).ifPresent(order -> {
                order.setTotalPrice(BigDecimal.ZERO);
                orderRepository.save(order);
            });
        }
    }

    private void clearLineItemsRestoreStock(UUID orderId) {
        List<OrderItem> items = orderItemRepository.findAllByOrder_OrderId(orderId);
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product != null && item.getQuantity() != null) {
                int current = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                product.setStockQuantity(current + item.getQuantity());
                productRepository.save(product);
            }
        }
        orderItemRepository.deleteAllByOrder_OrderId(orderId);
    }
}
