package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.service.CartMergeService;
import com.leang.authservice.service.CartOwnerResolver;
import com.leang.authservice.service.GuestSessionService;
import com.leang.authservice.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartMergeServiceImpl implements CartMergeService {

    private final CartOwnerResolver cartOwnerResolver;
    private final GuestSessionService guestSessionService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public OrderViewResponse mergeGuestCartIntoUser(HttpServletRequest request, HttpServletResponse response) {
        UUID userId = cartOwnerResolver.currentUserId()
                .orElseThrow(() -> new BadRequestException("Sign in required to merge cart"));

        UUID guestSessionId = guestSessionService.findValidSessionId(request).orElse(null);
        CartOwner userOwner = CartOwner.user(userId);
        Order userCart = orderService.findOrCreatePendingCart(userOwner);

        if (guestSessionId == null) {
            return toView(userCart);
        }

        UUID guestOrderId = orderService.findPendingCartId(CartOwner.guest(guestSessionId));
        if (guestOrderId == null || guestOrderId.equals(userCart.getOrderId())) {
            guestSessionService.clearSessionCookie(response);
            return toView(userCart);
        }

        Order guestCart = orderService.getById(guestOrderId);
        List<OrderItem> guestItems = orderItemRepository.findAllByOrder_OrderId(guestOrderId);
        if (guestItems.isEmpty()) {
            orderRepository.delete(guestCart);
            guestSessionService.clearSessionCookie(response);
            return toView(userCart);
        }

        Map<UUID, OrderItem> userLinesByProduct = new HashMap<>();
        for (OrderItem item : orderItemRepository.findAllByOrder_OrderId(userCart.getOrderId())) {
            if (item.getProduct() != null) {
                userLinesByProduct.put(item.getProduct().getProductId(), item);
            }
        }

        for (OrderItem guestItem : guestItems) {
            Product product = guestItem.getProduct();
            if (product == null) {
                continue;
            }
            OrderItem existing = userLinesByProduct.get(product.getProductId());
            if (existing != null) {
                int newQty = existing.getQuantity() + guestItem.getQuantity();
                existing.setQuantity(newQty);
                existing.setPrice(product.getPrice().multiply(BigDecimal.valueOf(newQty)));
                orderItemRepository.save(existing);
                orderItemRepository.delete(guestItem);
            } else {
                guestItem.setOrder(userCart);
                orderItemRepository.save(guestItem);
                userLinesByProduct.put(product.getProductId(), guestItem);
            }
        }

        userCart.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(userCart.getOrderId()));
        orderRepository.save(userCart);

        orderItemRepository.deleteAllByOrder_OrderId(guestOrderId);
        orderRepository.delete(guestCart);
        guestSessionService.clearSessionCookie(response);

        Order refreshed = orderService.getById(userCart.getOrderId());
        return toView(refreshed);
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
                order.getFulfillment(),
                order.getCustomerName(),
                order.getContactNumber()
        );
    }
}
