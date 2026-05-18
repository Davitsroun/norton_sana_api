package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.AdminOrderListItemResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.UserProfile;
import com.leang.authservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminOrderMapper {

    private final UserProfileRepository userProfileRepository;

    public AdminOrderListItemResponse toAdminListItem(Order order) {
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
        String avatar = profile != null ? profile.getAvatarUrl() : null;
        String status = order.getStatus() == null ? null : order.getStatus().toUpperCase(Locale.ROOT);
        return new AdminOrderListItemResponse(
                order.getOrderId(),
                customerName,
                email,
                order.getDeliveryAddress(),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getCurrency() != null ? order.getCurrency() : "USD",
                status,
                avatar
        );
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
}
