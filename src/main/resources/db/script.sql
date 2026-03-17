-- =========================
-- CATEGORY
-- =========================
INSERT INTO category (category_id, category_name)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Cleanser'),
    ('11111111-1111-1111-1111-111111111112', 'Toner'),
    ('11111111-1111-1111-1111-111111111113', 'Serum'),
    ('11111111-1111-1111-1111-111111111114', 'Moisturizer'),
    ('11111111-1111-1111-1111-111111111115', 'Sunscreen');
-- =========================
-- BRAND
-- =========================
INSERT INTO brand (brand_id, brand_name, country)
VALUES
    ('22222222-2222-2222-2222-222222222221', 'CeraVe', 'USA'),
    ('22222222-2222-2222-2222-222222222222', 'La Roche-Posay', 'France'),
    ('22222222-2222-2222-2222-222222222223', 'The Ordinary', 'Canada'),
    ('22222222-2222-2222-2222-222222222224', 'COSRX', 'South Korea'),
    ('22222222-2222-2222-2222-222222222225', 'Paula''s Choice', 'USA');
-- =========================
-- PRODUCT
-- =========================
INSERT INTO product (
    product_id, name, description, price, stock_quantity,
    category_id, brand_id, image_url, created_at
) VALUES
      -- Cleansers
      ('33333333-3333-3333-3333-333333333331',
       'CeraVe Hydrating Cleanser',
       'Gentle non-foaming cleanser with ceramides and hyaluronic acid for normal to dry skin.',
       14.99, 120,
       '11111111-1111-1111-1111-111111111111',
       '22222222-2222-2222-2222-222222222221',
       'https://example.com/images/cerave-hydrating-cleanser.jpg',
       NOW()),
      ('33333333-3333-3333-3333-333333333332',
       'COSRX Low pH Good Morning Gel Cleanser',
       'Mild gel cleanser with low pH suitable for daily use and sensitive skin.',
       12.50, 150,
       '11111111-1111-1111-1111-111111111111',
       '22222222-2222-2222-2222-222222222224',
       'https://example.com/images/cosrx-good-morning-cleanser.jpg',
       NOW()),
      -- Toners
      ('33333333-3333-3333-3333-333333333333',
       'La Roche-Posay Effaclar Clarifying Solution',
       'Micro-exfoliating toner with salicylic acid for oily and acne-prone skin.',
       18.90, 80,
       '11111111-1111-1111-1111-111111111112',
       '22222222-2222-2222-2222-222222222222',
       'https://example.com/images/laroche-effaclar-toner.jpg',
       NOW()),
      -- Serums
      ('33333333-3333-3333-3333-333333333334',
       'The Ordinary Niacinamide 10% + Zinc 1%',
       'High-strength vitamin and mineral blemish formula for oil control and pore appearance.',
       7.50, 200,
       '11111111-1111-1111-1111-111111111113',
       '22222222-2222-2222-2222-222222222223',
       'https://example.com/images/ordinary-niacinamide.jpg',
       NOW()),
      ('33333333-3333-3333-3333-333333333335',
       'Paula''s Choice 2% BHA Liquid Exfoliant',
       'Leave-on exfoliant with salicylic acid that unclogs pores and smooths wrinkles.',
       32.00, 60,
       '11111111-1111-1111-1111-111111111113',
       '22222222-2222-2222-2222-222222222225',
       'https://example.com/images/paulaschoice-bha.jpg',
       NOW()),
      -- Moisturizers
      ('33333333-3333-3333-3333-333333333336',
       'CeraVe Moisturizing Cream',
       'Rich, non-greasy moisturizer with ceramides and hyaluronic acid for dry skin.',
       16.99, 100,
       '11111111-1111-1111-1111-111111111114',
       '22222222-2222-2222-2222-222222222221',
       'https://example.com/images/cerave-moisturizing-cream.jpg',
       NOW()),
      -- Sunscreen
      ('33333333-3333-3333-3333-333333333337',
       'La Roche-Posay Anthelios Invisible Fluid SPF50+',
       'Lightweight, non-greasy sunscreen with broad spectrum protection.',
       29.90, 90,
       '11111111-1111-1111-1111-111111111115',
       '22222222-2222-2222-2222-222222222222',
       'https://example.com/images/laroche-anthelios-spf50.jpg',
       NOW());
-- =========================
-- ORDERS (Keycloak user IDs as UUIDs)
-- Assume these user IDs come from Keycloak (sub claim)
-- user A and user B as examples
-- =========================
INSERT INTO orders (order_id, user_id, total_price, status, created_at)
VALUES
    ('44444444-4444-4444-4444-444444444441',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     47.89,
     'Paid',
     NOW() - INTERVAL '3 days'),
    ('44444444-4444-4444-4444-444444444442',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     62.90,
     'Shipped',
     NOW() - INTERVAL '1 days');
-- =========================
-- ORDER ITEMS
-- =========================
INSERT INTO order_item (order_item_id, order_id, product_id, quantity, price)
VALUES
    -- Order 1 (user A)
    ('55555555-5555-5555-5555-555555555551',
     '44444444-4444-4444-4444-444444444441',
     '33333333-3333-3333-3333-333333333331',  -- CeraVe cleanser
     1,
     14.99),
    ('55555555-5555-5555-5555-555555555552',
     '44444444-4444-4444-4444-444444444441',
     '33333333-3333-3333-3333-333333333334',  -- The Ordinary Niacinamide
     2,
     7.50),
    -- Order 2 (user B)
    ('55555555-5555-5555-5555-555555555553',
     '44444444-4444-4444-4444-444444444442',
     '33333333-3333-3333-3333-333333333336',  -- CeraVe Moisturizing Cream
     1,
     16.99),
    ('55555555-5555-5555-5555-555555555554',
     '44444444-4444-4444-4444-444444444442',
     '33333333-3333-3333-3333-333333333337',  -- La Roche-Posay sunscreen
     1,
     29.90);
-- =========================
-- PAYMENTS
-- =========================
INSERT INTO payment (
    payment_id, order_id, payment_method,
    payment_status, transaction_id, paid_at
) VALUES
      ('66666666-6666-6666-6666-666666666661',
       '44444444-4444-4444-4444-444444444441',
       'Credit Card',
       'Completed',
       'TXN-20260313-0001',
       NOW() - INTERVAL '3 days'),
      ('66666666-6666-6666-6666-666666666662',
       '44444444-4444-4444-4444-444444444442',
       'ABA Pay',
       'Completed',
       'TXN-20260315-0002',
       NOW() - INTERVAL '1 days');
-- =========================
-- REVIEWS
-- =========================
INSERT INTO review (
    review_id, user_id, product_id,
    rating, comment, created_at
) VALUES
      ('77777777-7777-7777-7777-777777777771',
       'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
       '33333333-3333-3333-3333-333333333331', -- CeraVe Hydrating Cleanser
       5,
       'My skin feels clean and hydrated without any tightness. Great for daily use.',
       NOW() - INTERVAL '5 days'),
      ('77777777-7777-7777-7777-777777777772',
       'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
       '33333333-3333-3333-3333-333333333337', -- La Roche-Posay SPF50
       4,
       'Very lightweight texture and no white cast. A bit pricey but worth it.',
       NOW() - INTERVAL '2 days'),
      ('77777777-7777-7777-7777-777777777773',
       'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
       '33333333-3333-3333-3333-333333333334', -- The Ordinary Niacinamide
       5,
       'Helped with oiliness and reduced the appearance of my pores after a few weeks.',
       NOW() - INTERVAL '7 days');