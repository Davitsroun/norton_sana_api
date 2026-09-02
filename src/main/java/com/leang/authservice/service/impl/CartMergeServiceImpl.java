package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.service.CartLineMerger;
import com.leang.authservice.service.CartMergeService;
import com.leang.authservice.service.CartOwnerResolver;
import com.leang.authservice.service.GuestSessionService;
import com.leang.authservice.service.OrderService;
import com.leang.authservice.service.OrderViewMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartMergeServiceImpl implements CartMergeService {

    private final CartOwnerResolver cartOwnerResolver;
    private final GuestSessionService guestSessionService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderViewMapper orderViewMapper;
    private final CartLineMerger cartLineMerger;

    @Override
    @Transactional
    public OrderViewResponse mergeGuestCartIntoUser(HttpServletRequest request, HttpServletResponse response) {
        UUID userId = cartOwnerResolver.currentUserId()
                .orElseThrow(() -> new BadRequestException("Sign in required to merge cart"));
        Order merged = mergeGuestIntoUserCart(userId, request, response);
        return orderViewMapper.toView(merged);
    }

    @Override
    @Transactional
    public void mergeGuestCartIfPresent(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        if (userId == null) {
            return;
        }
        mergeGuestIntoUserCart(userId, request, response);
    }

    private Order mergeGuestIntoUserCart(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        UUID guestSessionId = guestSessionService.findValidSessionId(request).orElse(null);
        CartOwner userOwner = CartOwner.user(userId);
        Order userCart = orderService.findOrCreatePendingCart(userOwner);

        if (guestSessionId == null) {
            return userCart;
        }

        UUID guestOrderId = orderService.findPendingCartId(CartOwner.guest(guestSessionId));
        if (guestOrderId == null || guestOrderId.equals(userCart.getOrderId())) {
            guestSessionService.clearSessionCookie(response);
            return userCart;
        }

        Order guestCart = orderService.getById(guestOrderId);
        List<OrderItem> guestItems = orderItemRepository.findAllByOrder_OrderId(guestOrderId);
        if (guestItems.isEmpty()) {
            orderRepository.delete(guestCart);
            guestSessionService.clearSessionCookie(response);
            return userCart;
        }

        cartLineMerger.mergeLinesInto(userCart, guestItems);

        orderItemRepository.deleteAllByOrder_OrderId(guestOrderId);
        orderRepository.delete(guestCart);
        guestSessionService.clearSessionCookie(response);

        return orderService.getById(userCart.getOrderId());
    }
}
