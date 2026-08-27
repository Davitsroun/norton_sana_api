package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.AdminOrderDetailResponse;
import com.leang.authservice.model.dto.response.AdminOrderListItemResponse;
import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AdminOrderMapper {

    private final UserProfileRepository userProfileRepository;

    public AdminOrderListItemResponse toAdminListItem(Order order) {
        ResolvedCustomer customer = resolveCustomer(order);
        String status = order.getStatus() == null ? null : order.getStatus().toUpperCase(Locale.ROOT);
        return new AdminOrderListItemResponse(
                order.getOrderId(),
                customer.name(),
                customer.email(),
                order.getDeliveryAddress(),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getCurrency() != null ? order.getCurrency() : "USD",
                status,
                customer.avatar()
        );
    }

    public AdminOrderDetailResponse toAdminDetail(Order order) {
        ResolvedCustomer customer = resolveCustomer(order);
        String status = order.getStatus() == null ? null : order.getStatus().toUpperCase(Locale.ROOT);
        return new AdminOrderDetailResponse(
                order.getOrderId(),
                customer.name(),
                customer.email() != null ? customer.email() : order.getGuestEmail(),
                order.getDeliveryAddress(),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getCurrency() != null ? order.getCurrency() : "USD",
                status,
                customer.avatar(),
                order.getContactNumber(),
                order.getPaymentMethod(),
                order.getFulfillment(),
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
        return new OrderLineViewResponse(
                item.getOrderItemId(),
                productName,
                item.getQuantity(),
                item.getPrice(),
                image
        );
    }

    private ResolvedCustomer resolveCustomer(Order order) {
        UserProfile profile = null;
        if (order.getUserId() != null) {
            profile = userProfileRepository.findByKeycloakId(order.getUserId().toString()).orElse(null);
        }
        String customerName = order.getCustomerName();
        if ((customerName == null || customerName.isBlank()) && profile != null) {
            customerName = joinName(profile.getFirstName(), profile.getLastName());
            if (customerName.isBlank()) {
                customerName = profile.getUsername();
            }
        }
        String email = profile != null ? profile.getEmail() : null;
        if ((email == null || email.isBlank()) && order.getGuestEmail() != null) {
            email = order.getGuestEmail();
        }
        String avatar = profile != null ? profile.getAvatarUrl() : null;
        return new ResolvedCustomer(customerName, email, avatar);
    }

    private static String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
    }

    private record ResolvedCustomer(String name, String email, String avatar) {
    }
}
