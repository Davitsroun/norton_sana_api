package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.request.OrderCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.service.CartLineMerger;
import com.leang.authservice.service.OrderService;
import com.leang.authservice.util.OrderStatuses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartLineMerger cartLineMerger;

    @Override
    public Order create(OrderCreateRequest dto) {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getClaimAsString("sub");
        String status = dto.getStatus() == null ? OrderStatuses.PENDING : dto.getStatus().name().toLowerCase();
        Order order = Order.builder()
                .userId(UUID.fromString(userId))
                .totalPrice(null)
                .status(status)
                .createdAt(Instant.now())
                .build();
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order createPendingCart(CartOwner owner) {
        if (owner == null || (!owner.isRegistered() && !owner.isGuest())) {
            throw new BadRequestException("Cart owner required");
        }
        Order order = Order.builder()
                .userId(owner.userId())
                .sessionId(owner.sessionId())
                .totalPrice(BigDecimal.ZERO)
                .status(OrderStatuses.PENDING)
                .currency("USD")
                .createdAt(Instant.now())
                .build();
        return orderRepository.save(order);
    }

    @Override
    public Order update(UUID id, Order order) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        existing.setStatus(order.getStatus());
        existing.setTotalPrice(order.getTotalPrice());
        existing.setUserId(order.getUserId());
        return orderRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        orderRepository.delete(existing);
    }

    @Override
    public Order getById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @Override
    public ApiResponseWithPagination<Order> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                orderPage.getContent(),
                page,
                size,
                (int) orderPage.getTotalElements()
        );
    }

    @Override
    public UUID getUserOrder() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getClaimAsString("sub");
        return findPendingCartId(CartOwner.user(UUID.fromString(userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findPendingCartId(CartOwner owner) {
        List<UUID> ids;
        if (owner.isRegistered()) {
            ids = orderRepository.findPendingOrderIdsByUserId(owner.userId());
        } else if (owner.isGuest()) {
            ids = orderRepository.findPendingOrderIdsBySessionId(owner.sessionId());
        } else {
            return null;
        }
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    @Transactional
    public Order findOrCreatePendingCart(CartOwner owner) {
        List<Order> active = findActiveCarts(owner);
        if (active.isEmpty()) {
            return createPendingCart(owner);
        }
        return active.get(0);
    }

    @Override
    @Transactional
    public List<Order> findActiveCarts(CartOwner owner) {
        List<Order> active;
        if (owner.isRegistered()) {
            active = orderRepository.findActiveOrdersForUser(owner.userId());
        } else if (owner.isGuest()) {
            active = orderRepository.findActiveOrdersForSession(owner.sessionId());
        } else {
            return List.of();
        }
        if (active.size() <= 1) {
            active.forEach(o -> cartLineMerger.consolidateDuplicateProductsInOrder(o.getOrderId()));
            return reloadWithDetails(active);
        }
        Order consolidated = cartLineMerger.consolidateActiveOrders(active);
        cartLineMerger.consolidateDuplicateProductsInOrder(consolidated.getOrderId());
        return reloadWithDetails(List.of(consolidated));
    }

    private List<Order> reloadWithDetails(List<Order> orders) {
        return orders.stream()
                .map(o -> orderRepository.findWithDetailsByOrderId(o.getOrderId()).orElse(o))
                .toList();
    }
}
