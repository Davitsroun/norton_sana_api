package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.UserNotificationCreateRequest;
import com.leang.authservice.model.dto.request.UserNotificationUpdateRequest;
import com.leang.authservice.model.dto.response.UserNotificationResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Payment;
import com.leang.authservice.model.entity.UserNotification;
import com.leang.authservice.repository.UserNotificationRepository;
import com.leang.authservice.service.CurrentUserService;
import com.leang.authservice.service.NotificationService;
import com.leang.authservice.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationServiceImpl implements UserNotificationService {

    public static final String TYPE_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";

    private final UserNotificationRepository userNotificationRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void notifyPaymentSuccessOnce(Order order, Payment payment) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        UUID userId = order.getUserId();
        UUID orderId = order.getOrderId();
        if (userNotificationRepository.existsByUserIdAndOrderIdAndType(userId, orderId, TYPE_PAYMENT_SUCCESS)) {
            return;
        }
        BigDecimal total = order.getTotalPrice();
        String currency = order.getCurrency() != null ? order.getCurrency() : "";
        String title = "Payment successful";
        String body = String.format(
                "Your payment for order %s was successful. Total: %s %s.",
                orderId,
                total != null ? total.toPlainString() : "0",
                currency
        );
        UserNotification row = UserNotification.builder()
                .userId(userId)
                .type(TYPE_PAYMENT_SUCCESS)
                .title(title)
                .body(body)
                .orderId(orderId)
                .paymentId(payment != null ? payment.getPaymentId() : null)
                .read(false)
                .createdAt(Instant.now())
                .build();
        userNotificationRepository.save(row);

        try {
            notificationService.sendMessageToUser(userId.toString(), body);
        } catch (IllegalStateException ex) {
            log.warn("OneSignal not configured or invalid; notification saved only in DB: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("OneSignal push failed; notification saved in DB", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationResponse> listMine() {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        return userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserNotificationResponse getMine(UUID id) {
        return toResponse(requireOwned(id));
    }

    @Override
    @Transactional
    public UserNotificationResponse createMine(UserNotificationCreateRequest request) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        UserNotification row = UserNotification.builder()
                .userId(userId)
                .type(request.type().trim())
                .title(request.title().trim())
                .body(request.body().trim())
                .read(false)
                .createdAt(Instant.now())
                .build();
        return toResponse(userNotificationRepository.save(row));
    }

    @Override
    @Transactional
    public UserNotificationResponse updateMine(UUID id, UserNotificationUpdateRequest request) {
        UserNotification existing = requireOwned(id);
        if (request.read() != null) {
            existing.setRead(request.read());
        }
        if (request.title() != null && !request.title().isBlank()) {
            existing.setTitle(request.title().trim());
        }
        if (request.body() != null && !request.body().isBlank()) {
            existing.setBody(request.body().trim());
        }
        return toResponse(userNotificationRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteMine(UUID id) {
        UserNotification existing = requireOwned(id);
        userNotificationRepository.delete(existing);
    }

    private UserNotification requireOwned(UUID notificationId) {
        UUID userId = UUID.fromString(currentUserService.keycloakSub());
        return userNotificationRepository.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    private UserNotificationResponse toResponse(UserNotification n) {
        return new UserNotificationResponse(
                n.getNotificationId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getOrderId(),
                n.getPaymentId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
