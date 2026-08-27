package com.leang.authservice.service;

import java.util.UUID;

/**
 * After a successful payment, clears other unpaid "cart" orders for the same user
 * or guest session (restores product stock and removes line items).
 */
public interface CartCleanupService {

    void clearOtherActiveOrdersExcept(UUID userId, UUID paidOrderId);

    void clearOtherActiveOrdersExcept(UUID userId, UUID sessionId, UUID paidOrderId);
}
