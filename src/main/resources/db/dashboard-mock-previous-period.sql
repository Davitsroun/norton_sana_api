-- Mock dashboard data for the *previous* rolling 30-day window (31–60 days ago).
-- Safe to re-run: deletes rows with the fixed UUIDs below, then re-inserts.
--
-- Affects:
--   GET /api/v1/admin/dashboard/summary  (revenue / orders / users deltas)
--   GET /api/v1/admin/dashboard/revenue-chart  (month bucket for prior period)

-- =========================
-- Cleanup (idempotent)
-- =========================
DELETE FROM order_item WHERE order_item_id IN (
    'cccccccc-0001-0001-0001-000000000001',
    'cccccccc-0001-0001-0001-000000000002',
    'cccccccc-0001-0001-0001-000000000003',
    'cccccccc-0001-0001-0001-000000000004',
    'cccccccc-0001-0001-0001-000000000005',
    'cccccccc-0001-0001-0001-000000000006'
);
DELETE FROM payment WHERE payment_id IN (
    'dddddddd-0001-0001-0001-000000000001',
    'dddddddd-0001-0001-0001-000000000002',
    'dddddddd-0001-0001-0001-000000000003'
);
DELETE FROM orders WHERE order_id IN (
    'bbbbbbbb-0001-0001-0001-000000000001',
    'bbbbbbbb-0001-0001-0001-000000000002',
    'bbbbbbbb-0001-0001-0001-000000000003'
);
DELETE FROM user_profile WHERE id IN (
    'aaaaaaaa-0001-0001-0001-000000000001',
    'aaaaaaaa-0001-0001-0001-000000000002',
    'aaaaaaaa-0001-0001-0001-000000000003'
);

-- =========================
-- Mock users (previous 30-day window)
-- =========================
INSERT INTO user_profile (id, keycloak_id, email, username, first_name, last_name, created_at, updated_at)
VALUES
    ('aaaaaaaa-0001-0001-0001-000000000001', 'mock-kc-dashboard-001', 'mock.prev1@dashboard.test', 'mock_prev1', 'Sok', 'Chan', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '52 days'), (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '52 days')),
    ('aaaaaaaa-0001-0001-0001-000000000002', 'mock-kc-dashboard-002', 'mock.prev2@dashboard.test', 'mock_prev2', 'Dara', 'Kim', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '47 days'), (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '47 days')),
    ('aaaaaaaa-0001-0001-0001-000000000003', 'mock-kc-dashboard-003', 'mock.prev3@dashboard.test', 'mock_prev3', 'Vanna', 'Lim', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '42 days'), (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '42 days'));

-- =========================
-- Mock paid orders (previous 30-day window) — total revenue 258.50
-- =========================
INSERT INTO orders (
    order_id, user_id, total_price, status, currency, payment_method, fulfillment,
    delivery_address, customer_name, contact_number, created_at
) VALUES
    ('bbbbbbbb-0001-0001-0001-000000000001', 'aaaaaaaa-0001-0001-0001-000000000001', 89.50, 'paid', 'USD', 'BAKONG', 'delivery',
     'Phnom Penh, Cambodia', 'Sok Chan', '+85590000001', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '55 days')),
    ('bbbbbbbb-0001-0001-0001-000000000002', 'aaaaaaaa-0001-0001-0001-000000000002', 124.00, 'paid', 'USD', 'BAKONG', 'pickup',
     NULL, 'Dara Kim', '+85590000002', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '48 days')),
    ('bbbbbbbb-0001-0001-0001-000000000003', 'aaaaaaaa-0001-0001-0001-000000000003', 45.00, 'completed', 'USD', 'CARD', 'delivery',
     'Siem Reap, Cambodia', 'Vanna Lim', '+85590000003', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '41 days'));

INSERT INTO order_item (order_item_id, order_id, product_id, quantity, price)
VALUES
    ('cccccccc-0001-0001-0001-000000000001', 'bbbbbbbb-0001-0001-0001-000000000001', '33333333-3333-3333-3333-333333333335', 2, 39.99),
    ('cccccccc-0001-0001-0001-000000000002', 'bbbbbbbb-0001-0001-0001-000000000001', '33333333-3333-3333-3333-333333333334', 1, 7.50),
    ('cccccccc-0001-0001-0001-000000000003', 'bbbbbbbb-0001-0001-0001-000000000002', '33333333-3333-3333-3333-333333333337', 3, 24.00),
    ('cccccccc-0001-0001-0001-000000000004', 'bbbbbbbb-0001-0001-0001-000000000002', '33333333-3333-3333-3333-333333333336', 2, 16.99),
    ('cccccccc-0001-0001-0001-000000000005', 'bbbbbbbb-0001-0001-0001-000000000003', '33333333-3333-3333-3333-333333333333', 2, 21.00),
    ('cccccccc-0001-0001-0001-000000000006', 'bbbbbbbb-0001-0001-0001-000000000003', '33333333-3333-3333-3333-333333333332', 1, 12.50);

INSERT INTO payment (payment_id, order_id, payment_method, payment_status, transaction_id, paid_at)
VALUES
    ('dddddddd-0001-0001-0001-000000000001', 'bbbbbbbb-0001-0001-0001-000000000001', 'BAKONG', 'SUCCESS', 'mock-txn-prev-001', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '55 days')),
    ('dddddddd-0001-0001-0001-000000000002', 'bbbbbbbb-0001-0001-0001-000000000002', 'BAKONG', 'SUCCESS', 'mock-txn-prev-002', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '48 days')),
    ('dddddddd-0001-0001-0001-000000000003', 'bbbbbbbb-0001-0001-0001-000000000003', 'CARD', 'SUCCESS', 'mock-txn-prev-003', (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - INTERVAL '41 days'));
