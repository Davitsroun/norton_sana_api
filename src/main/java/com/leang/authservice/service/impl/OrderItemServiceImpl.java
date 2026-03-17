package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    @Override
    public OrderItem create(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    @Override
    public OrderItem update(UUID id, OrderItem orderItem) {
        OrderItem existing = orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order item not found"));
        existing.setOrder(orderItem.getOrder());
        existing.setProduct(orderItem.getProduct());
        existing.setQuantity(orderItem.getQuantity());
        existing.setPrice(orderItem.getPrice());
        return orderItemRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        OrderItem existing = orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order item not found"));
        orderItemRepository.delete(existing);
    }

    @Override
    public OrderItem getById(UUID id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order item not found"));
    }

    @Override
    public ApiResponseWithPagination<OrderItem> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<OrderItem> orderItemPage = orderItemRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                orderItemPage.getContent(),
                page,
                size,
                (int) orderItemPage.getTotalElements()
        );
    }
}

