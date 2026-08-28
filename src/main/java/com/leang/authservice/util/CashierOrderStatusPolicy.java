package com.leang.authservice.util;

import java.util.Locale;
import java.util.Set;

/**
 * Which order statuses cashiers may set via PATCH (fulfillment handover, not refunds).
 */
public final class CashierOrderStatusPolicy {

    private static final Set<String> ALLOWED = Set.of(
            "processing",
            "paid",
            "completed",
            "shipped",
            "ready",
            "ready_for_pickup",
            "dispatched"
    );

    private CashierOrderStatusPolicy() {
    }

    public static void assertCashierMaySet(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Cashiers may only set status to: processing, paid, completed, shipped, ready, ready_for_pickup, dispatched"
            );
        }
    }
}
