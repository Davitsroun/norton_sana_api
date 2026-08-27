package com.leang.authservice.util;

/**
 * Canonical lowercase order status strings used across cart / checkout.
 */
public final class OrderStatuses {

    public static final String PENDING = "pending";
    public static final String PROCESSING = "processing";
    public static final String PAID = "paid";
    public static final String COMPLETED = "completed";
    public static final String SHIPPED = "shipped";

    private OrderStatuses() {
    }
}
