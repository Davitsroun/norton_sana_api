package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.OrderItemCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.OrderItem;

import java.util.UUID;

public interface OrderItemService {

    OrderItem create(OrderItemCreateRequest d);

    OrderItem update(UUID id, OrderItem orderItem);

    void delete(UUID id);

    OrderItem getById(UUID id);

    ApiResponseWithPagination<OrderItem> getAll(int page, int size);
}

