-- Mock product batches for Norton Skincare seed catalog (script.sql product IDs 333...331–358)
-- Safe to re-run: replaces MOCK-* / LEGACY batches on seed products, then syncs product.stock_quantity.
-- Run after migrate-product-batches.sql (or Hibernate ddl-auto).
-- Usage (Docker):
--   docker exec -i auth-service-spring-mini-project-group-03-api_db-1 psql -U David -d api_db < src/main/resources/db/mock-product-batches.sql

BEGIN;

-- Remove prior mock/legacy batches (allocations first if any exist on mock batches)
DELETE FROM order_item_batch_allocation
WHERE batch_id IN (
    SELECT id FROM product_batch
    WHERE batch_code LIKE 'MOCK-%' OR batch_code = 'LEGACY'
);

DELETE FROM product_batch
WHERE batch_code LIKE 'MOCK-%' OR batch_code = 'LEGACY';

-- Helper: cost ≈ 42% of product price (matches mock-product-cost-and-profile.sql)
-- Dates use CURRENT_DATE (Asia/Phnom_Penh alignment handled by app; SQL uses DB local date)

-- ─── 334 The Ordinary Niacinamide — FEFO demo: 10 soon + 50 later + 5 expired in warehouse
INSERT INTO product_batch (id, product_id, batch_code, expiry_date, received_date, quantity, initial_quantity, cost_price, status, created_at, updated_at)
VALUES
    ('b4444444-4444-4444-4444-444444444401', '33333333-3333-3333-3333-333333333334', 'MOCK-LOT-2026-A',
     (CURRENT_DATE + INTERVAL '14 days')::date, (CURRENT_DATE - INTERVAL '30 days')::date,
     10, 10, 3.15, 'ACTIVE', NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444402', '33333333-3333-3333-3333-333333333334', 'MOCK-LOT-2027-B',
     DATE '2027-01-15', (CURRENT_DATE - INTERVAL '7 days')::date,
     50, 50, 3.15, 'ACTIVE', NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444403', '33333333-3333-3333-3333-333333333334', 'MOCK-LOT-2025-X',
     (CURRENT_DATE - INTERVAL '60 days')::date, (CURRENT_DATE - INTERVAL '400 days')::date,
     5, 20, 3.15, 'EXPIRED', NOW(), NOW());

-- ─── 335 La Roche Hyalu B5 — low stock alert (3 sellable)
INSERT INTO product_batch VALUES
    ('b4444444-4444-4444-4444-444444444404', '33333333-3333-3333-3333-333333333335', 'MOCK-LOT-2026-LRP',
     (CURRENT_DATE + INTERVAL '8 months')::date, CURRENT_DATE,
     3, 70, 16.80, 'ACTIVE', NOW(), NOW());

-- ─── 340 Paula''s BHA — all batches expired → 0 sellable (hidden from shop)
INSERT INTO product_batch VALUES
    ('b4444444-4444-4444-4444-444444444405', '33333333-3333-3333-3333-333333333340', 'MOCK-LOT-2024-BHA',
     (CURRENT_DATE - INTERVAL '90 days')::date, (CURRENT_DATE - INTERVAL '500 days')::date,
     25, 60, 13.44, 'EXPIRED', NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444406', '33333333-3333-3333-3333-333333333340', 'MOCK-LOT-2025-BHA2',
     (CURRENT_DATE - INTERVAL '10 days')::date, (CURRENT_DATE - INTERVAL '200 days')::date,
     8, 8, 13.44, 'EXPIRED', NOW(), NOW());

-- ─── 331 CeraVe Cleanser — two active batches (80 + 40 = 120)
INSERT INTO product_batch VALUES
    ('b4444444-4444-4444-4444-444444444407', '33333333-3333-3333-3333-333333333331', 'MOCK-LOT-2026-CV1',
     (CURRENT_DATE + INTERVAL '20 days')::date, (CURRENT_DATE - INTERVAL '14 days')::date,
     80, 80, 6.30, 'ACTIVE', NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444408', '33333333-3333-3333-3333-333333333331', 'MOCK-LOT-2027-CV2',
     DATE '2027-06-01', CURRENT_DATE,
     40, 40, 6.30, 'ACTIVE', NOW(), NOW());

-- ─── 3355 Beauty of Joseon Glow Serum — expiring within 30 days (15 units)
INSERT INTO product_batch VALUES
    ('b4444444-4444-4444-4444-444444444409', '33333333-3333-3333-3333-333333333355', 'MOCK-LOT-2026-BOJ',
     (CURRENT_DATE + INTERVAL '22 days')::date, (CURRENT_DATE - INTERVAL '60 days')::date,
     15, 40, 7.77, 'ACTIVE', NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444410', '33333333-3333-3333-3333-333333333355', 'MOCK-LOT-2027-BOJ2',
     DATE '2027-03-01', CURRENT_DATE,
     110, 110, 7.77, 'ACTIVE', NOW(), NOW());

-- ─── Remaining seed products: one active batch each (sellable qty = original script.sql stock)
INSERT INTO product_batch (id, product_id, batch_code, expiry_date, received_date, quantity, initial_quantity, cost_price, status, created_at, updated_at)
SELECT
    gen_random_uuid(),
    p.product_id,
    'MOCK-LOT-DEFAULT',
    (CURRENT_DATE + INTERVAL '18 months')::date,
    CURRENT_DATE,
    GREATEST(COALESCE(p.stock_quantity, 0), 0),
    GREATEST(COALESCE(p.stock_quantity, 0), 0),
    GREATEST(ROUND(COALESCE(p.price, 0) * 0.42, 2), 0.01),
    CASE WHEN COALESCE(p.stock_quantity, 0) > 0 THEN 'ACTIVE' ELSE 'DEPLETED' END,
    NOW(),
    NOW()
FROM product p
WHERE p.product_id::text LIKE '33333333-3333-3333-3333-3333333333%'
  AND p.product_id NOT IN (
      '33333333-3333-3333-3333-333333333331',
      '33333333-3333-3333-3333-333333333334',
      '33333333-3333-3333-3333-333333333335',
      '33333333-3333-3333-3333-333333333340',
      '33333333-3333-3333-3333-333333333355'
  )
  AND NOT EXISTS (
      SELECT 1 FROM product_batch b WHERE b.product_id = p.product_id
  );

-- Sync cached sellable stock (non-expired ACTIVE batches only)
UPDATE product p
SET stock_quantity = COALESCE((
    SELECT SUM(b.quantity)
    FROM product_batch b
    WHERE b.product_id = p.product_id
      AND b.status = 'ACTIVE'
      AND b.expiry_date >= CURRENT_DATE
      AND b.quantity > 0
), 0)
WHERE p.product_id::text LIKE '33333333-3333-3333-3333-3333333333%';

COMMIT;

-- Quick sanity (optional):
-- SELECT p.name, p.stock_quantity AS sellable,
--        (SELECT COUNT(*) FROM product_batch b WHERE b.product_id = p.product_id) AS batches
-- FROM product p
-- WHERE p.product_id IN ('33333333-3333-3333-3333-333333333334','33333333-3333-3333-3333-333333333340')
-- ORDER BY p.name;
