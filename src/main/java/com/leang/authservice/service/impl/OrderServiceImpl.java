package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public Order create(Order order) {
        order.setCreatedAt(Instant.now());
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
}

