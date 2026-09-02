-- Product batch inventory migration
-- Run once against api_db (or rely on Hibernate ddl-auto: update for dev)

CREATE TABLE IF NOT EXISTS product_batch (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(product_id),
    batch_code VARCHAR(255) NOT NULL,
    expiry_date DATE NOT NULL,
    received_date DATE,
    quantity INT NOT NULL DEFAULT 0,
    initial_quantity INT,
    cost_price NUMERIC(19, 2),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_product_batch_code UNIQUE (product_id, batch_code)
);

CREATE TABLE IF NOT EXISTS order_item_batch_allocation (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_item(order_item_id),
    batch_id UUID NOT NULL REFERENCES product_batch(id),
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_product_batch_product_id ON product_batch(product_id);
CREATE INDEX IF NOT EXISTS idx_product_batch_expiry ON product_batch(expiry_date);
CREATE INDEX IF NOT EXISTS idx_order_item_batch_allocation_item ON order_item_batch_allocation(order_item_id);

-- Backfill: one LEGACY batch per product with existing flat stock
INSERT INTO product_batch (
    id, product_id, batch_code, expiry_date, received_date,
    quantity, initial_quantity, cost_price, status, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    p.product_id,
    'LEGACY',
    (CURRENT_DATE + INTERVAL '2 years')::date,
    CURRENT_DATE,
    COALESCE(p.stock_quantity, 0),
    COALESCE(p.stock_quantity, 0),
    p.cost_price,
    CASE
        WHEN COALESCE(p.stock_quantity, 0) <= 0 THEN 'DEPLETED'
        ELSE 'ACTIVE'
    END,
    NOW(),
    NOW()
FROM product p
WHERE COALESCE(p.stock_quantity, 0) > 0
  AND NOT EXISTS (
      SELECT 1 FROM product_batch b WHERE b.product_id = p.product_id
  );

-- Refresh cached sellable stock on product (non-expired active batches only)
UPDATE product p
SET stock_quantity = COALESCE((
    SELECT SUM(b.quantity)
    FROM product_batch b
    WHERE b.product_id = p.product_id
      AND b.status = 'ACTIVE'
      AND b.expiry_date >= CURRENT_DATE
      AND b.quantity > 0
), 0);
