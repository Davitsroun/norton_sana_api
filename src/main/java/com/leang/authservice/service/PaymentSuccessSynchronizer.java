package com.leang.authservice.service;

import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Payment;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * When payment is successful, marks the linked order as paid and normalizes payment status to SUCCESS.
 */
@Service
@RequiredArgsConstructor
public class PaymentSuccessSynchronizer {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserNotificationService userNotificationService;
    private final CartCleanupService cartCleanupService;

    public static boolean isSuccessfulPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String u = status.trim().toUpperCase(Locale.ROOT);
        return u.equals("PAID")
                || u.equals("SUCCESS")
                || u.equals("COMPLETED")
                || u.equals("SUCCEEDED");
    }

    /**
     * After a payment row is persisted (create/update), sync order + payment fields if status is successful.
     */
    @Transactional
    public void syncAfterPaymentPersisted(Payment payment) {
        if (!isSuccessfulPaymentStatus(payment.getPaymentStatus())) {
            return;
        }
        UUID orderId = payment.getOrder() != null ? payment.getOrder().getOrderId() : null;
        if (orderId == null) {
            return;
        }
        Order order = orderRepository.findById(orderId)
                .orElse(null);
        if (order == null) {
            return;
        }
        order.setStatus("paid");
        orderRepository.save(order);

        boolean paymentDirty = false;
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(Instant.now());
            paymentDirty = true;
        }
        if (!"SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
            payment.setPaymentStatus("SUCCESS");
            paymentDirty = true;
        }
        if (paymentDirty) {
            paymentRepository.save(payment);
        }
        Order refreshed = orderRepository.findById(orderId).orElse(order);
        userNotificationService.notifyPaymentSuccessOnce(refreshed, payment);
        cartCleanupService.clearOtherActiveOrdersExcept(refreshed.getUserId(), refreshed.getOrderId());
    }

    /**
     * Used by Bakong callback when upstream confirms a successful transaction.
     */
    @Transactional
    public void markOrderPaidFromGateway(UUID orderId, String transactionId, String defaultPaymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found for payment update"));
        order.setStatus("paid");
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseGet(() -> Payment.builder()
                        .order(order)
                        .paymentMethod(defaultPaymentMethod != null ? defaultPaymentMethod : "BAKONG")
                        .build());
        payment.setOrder(order);
        payment.setPaymentStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        Order refreshed = orderRepository.findById(orderId).orElse(order);
        userNotificationService.notifyPaymentSuccessOnce(refreshed, payment);
        cartCleanupService.clearOtherActiveOrdersExcept(refreshed.getUserId(), refreshed.getOrderId());
    }
}
