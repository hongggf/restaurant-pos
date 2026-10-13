-- 1. Add Prawn Dumplings to menu_items as a side dish.
INSERT INTO menu_items (id, name_en, name_km, description_en, description_km, price, currency_id, image_url, available, category_id, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'Prawn Dumplings',
    'គាវបង្គា',
    '',
    '',
    1.50,
    (SELECT id FROM currencies WHERE code = 'USD' LIMIT 1),
    '/food-images/prawn-dumplings.jpg',
    TRUE,
    (SELECT id FROM categories WHERE lower(name_en) = 'side dishes' LIMIT 1),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM menu_items WHERE lower(name_en) = 'prawn dumplings'
)
AND EXISTS (
    SELECT 1 FROM categories WHERE lower(name_en) = 'side dishes'
);

-- 2. Convert existing Prawn Dumplings cart modifiers into top-level cart lines
INSERT INTO cart_line_items (id, session_id, menu_item_id, quantity, remark, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    cli.session_id,
    (SELECT id FROM menu_items WHERE lower(name_en) = 'prawn dumplings' LIMIT 1),
    clms.quantity,
    NULL,
    NOW(),
    NOW()
FROM cart_line_modifier_selections clms
JOIN cart_line_items cli ON clms.cart_line_item_id = cli.id
WHERE clms.modifier_option_id IN (
    SELECT id FROM modifier_options WHERE lower(name_en) = 'prawn dumplings'
)
AND EXISTS (
    SELECT 1 FROM menu_items WHERE lower(name_en) = 'prawn dumplings'
);

-- 3. Remove Prawn Dumplings from modifier_options
DELETE FROM cart_line_modifier_selections
 WHERE modifier_option_id IN (
     SELECT id FROM modifier_options
      WHERE lower(name_en) = 'prawn dumplings'
 );

DELETE FROM modifier_options
 WHERE lower(name_en) = 'prawn dumplings';
