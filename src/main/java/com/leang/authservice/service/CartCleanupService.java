package com.leang.authservice.service;

import java.util.UUID;

/**
 * After a successful payment, clears other unpaid "cart" orders for the same user
 * (restores product stock and removes line items) so checkout data does not linger.
 */
public interface CartCleanupService {

    void clearOtherActiveOrdersExcept(UUID userId, UUID paidOrderId);
}
