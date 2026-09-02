package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.AdminOrderDetailResponse;
import com.leang.authservice.model.dto.response.AdminOrderListItemResponse;
import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Payment;
import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminOrderMapper {

    private final UserProfileRepository userProfileRepository;

    public AdminOrderListItemResponse toAdminListItem(Order order) {
        ResolvedCustomer customer = resolveCustomer(order);
        String status = normalizeStatus(order.getStatus());
        String fulfillment = normalizeFulfillment(order.getFulfillment());
        Payment payment = order.getPayment();
        return new AdminOrderListItemResponse(
                order.getOrderId(),
                order.getUserId(),
                customer.checkoutName(),
                customer.email(),
                order.getContactNumber(),
                fulfillment,
                order.getDeliveryAddress(),
                order.getFormattedAddress(),
                order.getLatitude(),
                order.getLongitude(),
                order.getProvince(),
                order.getDistrict(),
                order.getCommune(),
                order.getDeliveryInstructions(),
                order.getPickupNotes(),
                order.getPaymentMethod(),
                paymentStatus(payment),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getCurrency() != null ? order.getCurrency() : "USD",
                status,
                customer.avatar()
        );
    }

    public AdminOrderDetailResponse toAdminDetail(Order order) {
        ResolvedCustomer customer = resolveCustomer(order);
        String status = normalizeStatus(order.getStatus());
        String fulfillment = normalizeFulfillment(order.getFulfillment());
        Payment payment = order.getPayment();
        return new AdminOrderDetailResponse(
                order.getOrderId(),
                order.getUserId(),
                customer.checkoutName(),
                customer.email() != null ? customer.email() : order.getGuestEmail(),
                order.getContactNumber(),
                fulfillment,
                order.getDeliveryAddress(),
                order.getFormattedAddress(),
                order.getLatitude(),
                order.getLongitude(),
                order.getProvince(),
                order.getDistrict(),
                order.getCommune(),
                order.getPlaceId(),
                order.getDeliveryInstructions(),
                order.getPickupNotes(),
                order.getPaymentMethod(),
                paymentStatus(payment),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getCurrency() != null ? order.getCurrency() : "USD",
                status,
                customer.avatar(),
                order.getTrackingNumber(),
                order.getGuestEmail(),
                toLineItems(order)
        );
    }

    public List<OrderLineViewResponse> toLineItems(Order order) {
        if (order.getItems() == null) {
            return Collections.emptyList();
        }
        return order.getItems().stream()
                .map(this::toLineItem)
                .toList();
    }

    private OrderLineViewResponse toLineItem(OrderItem item) {
        String productName = item.getProduct() != null ? item.getProduct().getName() : null;
        String image = item.getProduct() != null ? item.getProduct().getImageUrl() : null;
        UUID productId = item.getProduct() != null ? item.getProduct().getProductId() : null;
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

    /**
     * Invoice name comes from checkout fulfillment ({@code order.customerName}) only.
     * Keycloak profile name is intentionally not used as a fallback.
     */
    private ResolvedCustomer resolveCustomer(Order order) {
        UserProfile profile = null;
        if (order.getUserId() != null) {
            profile = userProfileRepository.findByKeycloakId(order.getUserId().toString()).orElse(null);
        }
        String checkoutName = blankToNull(order.getCustomerName());
        String email = profile != null ? profile.getEmail() : null;
        if ((email == null || email.isBlank()) && order.getGuestEmail() != null) {
            email = order.getGuestEmail();
        }
        String avatar = profile != null ? profile.getAvatarUrl() : null;
        return new ResolvedCustomer(checkoutName, email, avatar);
    }

    private static String paymentStatus(Payment payment) {
        if (payment == null || payment.getPaymentStatus() == null) {
            return null;
        }
        return payment.getPaymentStatus().toUpperCase(Locale.ROOT);
    }

    private static String normalizeStatus(String status) {
        return status == null ? null : status.toUpperCase(Locale.ROOT);
    }

    private static String normalizeFulfillment(String fulfillment) {
        if (fulfillment == null || fulfillment.isBlank()) {
            return null;
        }
        return fulfillment.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ResolvedCustomer(String checkoutName, String email, String avatar) {
    }
}
