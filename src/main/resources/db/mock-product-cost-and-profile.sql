-- Mock COGS + profile fill so dashboard profit and admin users stay consistent.
-- Safe to re-run.

-- Product cost ≈ 42% of selling price (min $0.01). Tiny demo prices stay proportional.
UPDATE product
SET cost_price = GREATEST(
        ROUND(COALESCE(price, 0) * 0.42, 2),
        CASE WHEN COALESCE(price, 0) > 0 THEN 0.01 ELSE 0 END
    )
WHERE cost_price IS NULL OR cost_price = 0;

-- Snapshot unit cost on existing lines (historical profit uses this, not live product cost).
UPDATE order_item oi
SET unit_cost = COALESCE(p.cost_price, 0)
FROM product p
WHERE oi.product_id = p.product_id
  AND (oi.unit_cost IS NULL OR oi.unit_cost = 0);

-- Your admin/customer profile (both Keycloak IDs for sroundavit@gmail.com)
UPDATE user_profile
SET
    username = 'sroundavit',
    first_name = 'Sroun',
    last_name = 'Davit',
    phone = '+85512345678',
    avatar_url = 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=256&h=256&fit=crop',
    updated_at = NOW()
WHERE email = 'sroundavit@gmail.com';

-- Mock dashboard users: avatars so recent-orders / users list is not blank
UPDATE user_profile
SET
    phone = COALESCE(NULLIF(phone, ''), '+85590000001'),
    avatar_url = COALESCE(
        NULLIF(avatar_url, ''),
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=256&h=256&fit=crop'
    ),
    updated_at = NOW()
WHERE id = 'aaaaaaaa-0001-0001-0001-000000000001';

UPDATE user_profile
SET
    phone = COALESCE(NULLIF(phone, ''), '+85590000002'),
    avatar_url = COALESCE(
        NULLIF(avatar_url, ''),
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=256&h=256&fit=crop'
    ),
    updated_at = NOW()
WHERE id = 'aaaaaaaa-0001-0001-0001-000000000002';

UPDATE user_profile
SET
    phone = COALESCE(NULLIF(phone, ''), '+85590000003'),
    avatar_url = COALESCE(
        NULLIF(avatar_url, ''),
        'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=256&h=256&fit=crop'
    ),
    updated_at = NOW()
WHERE id = 'aaaaaaaa-0001-0001-0001-000000000003';
