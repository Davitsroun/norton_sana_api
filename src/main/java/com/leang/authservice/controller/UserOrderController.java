package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.CreateOrderRequest;
import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Transactional
    public OrderViewResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        Order order = Order.builder()
                .userId(userId)
                .status("pending")
                .currency("USD")
                .paymentMethod(request.paymentMethod())
                .fulfillment(request.fulfillment())
                .deliveryAddress(request.deliveryAddress())
                .createdAt(Instant.now())
                .totalPrice(BigDecimal.ZERO)
                .build();
        Order savedOrder = orderRepository.save(order);

        for (CreateOrderRequest.Item item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(item.quantity())
                    .price(lineTotal)
                    .build();
            orderItemRepository.save(orderItem);
        }

        savedOrder.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(savedOrder.getOrderId()));
        orderRepository.save(savedOrder);
        return toView(savedOrder);
    }

    @GetMapping
    public Page<OrderViewResponse> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toView);
    }

    @GetMapping("/{id}")
    public OrderViewResponse getMyOrderById(@PathVariable UUID id) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        Order order = orderRepository.findByOrderIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toView(order);
    }

    private OrderViewResponse toView(Order order) {
        List<OrderLineViewResponse> lines = order.getItems().stream()
                .map(item -> new OrderLineViewResponse(
                        item.getOrderItemId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getProduct().getImageUrl()
                ))
                .toList();
        return new OrderViewResponse(
                order.getOrderId(),
                order.getCreatedAt(),
                lines,
                order.getTotalPrice(),
                order.getStatus() == null ? null : order.getStatus().toLowerCase(),
                order.getTrackingNumber(),
                order.getPaymentMethod(),
                order.getFulfillment()
        );
    }
}
