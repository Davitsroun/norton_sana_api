package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class OrderViewMapper {

    public OrderViewResponse toView(Order order) {
        List<OrderLineViewResponse> lines = order.getItems() == null
                ? Collections.emptyList()
                : order.getItems().stream()
                .map(this::toLine)
                .toList();
        return new OrderViewResponse(
                order.getOrderId(),
                order.getCreatedAt(),
                lines,
                order.getTotalPrice(),
                order.getStatus() == null ? null : order.getStatus().toLowerCase(Locale.ROOT),
                order.getTrackingNumber(),
                order.getPaymentMethod(),
                order.getFulfillment(),
                order.getCustomerName(),
                order.getContactNumber(),
                order.getDeliveryAddress(),
                order.getLatitude(),
                order.getLongitude(),
                order.getProvince(),
                order.getDistrict(),
                order.getCommune(),
                order.getPlaceId(),
                order.getFormattedAddress(),
                order.getDeliveryInstructions(),
                order.getPickupNotes()
        );
    }

    private OrderLineViewResponse toLine(OrderItem item) {
        UUID productId = item.getProduct() != null ? item.getProduct().getProductId() : null;
        String productName = item.getProduct() != null ? item.getProduct().getName() : null;
        String image = item.getProduct() != null ? item.getProduct().getImageUrl() : null;
        BigDecimal unitPrice = unitPrice(item);
        return new OrderLineViewResponse(
                item.getOrderItemId(),
                productId,
                productName,
                item.getQuantity(),
                item.getPrice(),
                unitPrice,
                image
        );
    }

    private static BigDecimal unitPrice(OrderItem item) {
        if (item.getProduct() != null && item.getProduct().getPrice() != null) {
            return item.getProduct().getPrice();
        }
        if (item.getQuantity() != null && item.getQuantity() > 0 && item.getPrice() != null) {
            return item.getPrice().divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
        }
        return null;
    }
}
