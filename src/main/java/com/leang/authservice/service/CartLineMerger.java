package com.leang.authservice.service;

import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Merges order lines from a source cart into a target cart (same product → sum qty).
 */
@Component
@RequiredArgsConstructor
public class CartLineMerger {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public void mergeLinesInto(Order target, List<OrderItem> sourceLines) {
        if (sourceLines == null || sourceLines.isEmpty()) {
            return;
        }
        Map<UUID, OrderItem> targetByProduct = new HashMap<>();
        for (OrderItem item : orderItemRepository.findAllByOrder_OrderId(target.getOrderId())) {
            if (item.getProduct() != null) {
                targetByProduct.put(item.getProduct().getProductId(), item);
            }
        }

        for (OrderItem sourceLine : sourceLines) {
            Product product = sourceLine.getProduct();
            if (product == null) {
                continue;
            }
            OrderItem existing = targetByProduct.get(product.getProductId());
            if (existing != null) {
                int newQty = existing.getQuantity() + sourceLine.getQuantity();
                existing.setQuantity(newQty);
                existing.setPrice(product.getPrice().multiply(BigDecimal.valueOf(newQty)));
                orderItemRepository.save(existing);
                orderItemRepository.delete(sourceLine);
            } else {
                sourceLine.setOrder(target);
                orderItemRepository.save(sourceLine);
                targetByProduct.put(product.getProductId(), sourceLine);
            }
        }

        target.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(target.getOrderId()));
        orderRepository.save(target);
    }

    /**
     * Collapse duplicate rows for the same product on one order (qty 1 + qty 1 → qty 2).
     */
    public void consolidateDuplicateProductsInOrder(UUID orderId) {
        List<OrderItem> all = orderItemRepository.findAllByOrder_OrderId(orderId);
        if (all.size() <= 1) {
            return;
        }
        Map<UUID, List<OrderItem>> byProduct = new HashMap<>();
        for (OrderItem item : all) {
            if (item.getProduct() == null) {
                continue;
            }
            byProduct.computeIfAbsent(item.getProduct().getProductId(), k -> new java.util.ArrayList<>()).add(item);
        }
        boolean changed = false;
        for (List<OrderItem> lines : byProduct.values()) {
            if (lines.size() <= 1) {
                continue;
            }
            OrderItem primary = lines.get(0);
            Product product = primary.getProduct();
            int totalQty = lines.stream().mapToInt(OrderItem::getQuantity).sum();
            primary.setQuantity(totalQty);
            if (product != null && product.getPrice() != null) {
                primary.setPrice(product.getPrice().multiply(BigDecimal.valueOf(totalQty)));
            }
            orderItemRepository.save(primary);
            for (int i = 1; i < lines.size(); i++) {
                orderItemRepository.delete(lines.get(i));
            }
            changed = true;
        }
        if (changed) {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(orderId));
                orderRepository.save(order);
            });
        }
    }

    /**
     * Collapse multiple open carts into the newest non-empty one (or newest if all empty).
     */
    public Order consolidateActiveOrders(List<Order> activeOrders) {
        if (activeOrders == null || activeOrders.isEmpty()) {
            throw new IllegalArgumentException("No active orders to consolidate");
        }
        if (activeOrders.size() == 1) {
            return activeOrders.get(0);
        }

        List<Order> sorted = activeOrders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Order primary = sorted.stream()
                .filter(o -> o.getItems() != null && !o.getItems().isEmpty())
                .findFirst()
                .orElse(sorted.get(0));

        for (Order other : sorted) {
            if (other.getOrderId().equals(primary.getOrderId())) {
                continue;
            }
            List<OrderItem> otherLines = orderItemRepository.findAllByOrder_OrderId(other.getOrderId());
            mergeLinesInto(primary, otherLines);
            orderItemRepository.deleteAllByOrder_OrderId(other.getOrderId());
            orderRepository.delete(other);
        }

        return orderRepository.findById(primary.getOrderId()).orElse(primary);
    }
}
