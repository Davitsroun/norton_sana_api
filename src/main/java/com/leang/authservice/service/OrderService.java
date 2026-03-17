package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Order;

import java.util.UUID;

public interface OrderService {

    Order create(Order order);

    Order update(UUID id, Order order);

    void delete(UUID id);

    Order getById(UUID id);

    ApiResponseWithPagination<Order> getAll(int page, int size);
}

