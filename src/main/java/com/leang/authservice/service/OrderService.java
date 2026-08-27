package com.leang.authservice.service;

import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.request.OrderCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    Order create(OrderCreateRequest order);

    /** Create a pending cart for a user or guest session. */
    Order createPendingCart(CartOwner owner);

    Order update(UUID id, Order order);

    void delete(UUID id);

    Order getById(UUID id);

    ApiResponseWithPagination<Order> getAll(int page, int size);

    /** @deprecated use {@link #findPendingCartId(CartOwner)} */
    @Deprecated
    UUID getUserOrder();

    UUID findPendingCartId(CartOwner owner);

    Order findOrCreatePendingCart(CartOwner owner);

    List<Order> findActiveCarts(CartOwner owner);
}
