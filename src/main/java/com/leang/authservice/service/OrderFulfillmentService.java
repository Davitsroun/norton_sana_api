package com.leang.authservice.service;

import com.leang.authservice.exception.ConflictException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.request.OrderFulfillmentRequest;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.util.FulfillmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final FulfillmentApplier fulfillmentApplier;
    private final OrderViewMapper orderViewMapper;
    private final SavedLocationFulfillmentResolver savedLocationFulfillmentResolver;

    @Transactional
    public OrderViewResponse updateFulfillment(UUID orderId, UUID userId, OrderFulfillmentRequest request) {
        FulfillmentValidator.FulfillmentInput input = savedLocationFulfillmentResolver.resolve(
                request.deliveryOption(),
                request.fullName(),
                request.contactNumber(),
                request.deliveryAddress(),
                request.latitude(),
                request.longitude(),
                request.province(),
                request.district(),
                request.commune(),
                request.placeId(),
                request.formattedAddress(),
                request.deliveryInstructions(),
                request.pickupNotes(),
                request.savedLocationId()
        );
        FulfillmentValidator.validateAndThrow(input);
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        FulfillmentValidator.assertActiveCartStatus(order.getStatus());
        fulfillmentApplier.applyToOrder(order, input);
        return orderViewMapper.toView(orderRepository.save(order));
    }

    @Transactional
    public void syncOpenOrderFromProfile(UUID userId, FulfillmentValidator.FulfillmentInput input) {
        UUID orderId = orderService.findPendingCartId(CartOwner.user(userId));
        if (orderId == null) {
            return;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        try {
            FulfillmentValidator.assertActiveCartStatus(order.getStatus());
        } catch (ConflictException ignored) {
            return;
        }
        fulfillmentApplier.applyToOrder(order, input);
        orderRepository.save(order);
    }
}
