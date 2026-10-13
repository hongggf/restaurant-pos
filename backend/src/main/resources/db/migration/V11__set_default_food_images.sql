-- Populate image_url for standard menu items
UPDATE menu_items SET image_url = '/food-images/diy-malatang.jpg' WHERE lower(name_en) = 'diy malatang' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/combo-set.jpg' WHERE lower(name_en) LIKE '%football%' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/steamed-rice.jpg' WHERE lower(name_en) LIKE '%steamed rice%' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/sichuan-dumplings.jpg' WHERE lower(name_en) LIKE '%dumpling%' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/pineapple-tea.jpg' WHERE lower(name_en) LIKE '%pineapple%' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/apple-tea.jpg' WHERE lower(name_en) LIKE '%apple%' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/honey-lemon.jpg' WHERE lower(name_en) LIKE '%honey lemon%' AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/herbal-tea.jpg' WHERE (lower(name_en) LIKE '%jia duo bao%' OR lower(name_en) LIKE '%herbal%') AND (image_url IS NULL OR image_url = '');
UPDATE menu_items SET image_url = '/food-images/lollipop.jpg' WHERE lower(name_en) LIKE '%lollipop%' AND (image_url IS NULL OR image_url = '');

-- Populate image_url for modifier options
UPDATE modifier_options SET image_url = '/food-images/juicy-beef-balls.jpg' WHERE (lower(name_en) LIKE '%beef ball%' OR lower(name_en) LIKE '%tendon ball%') AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/fish-roe-meatballs.jpg' WHERE (lower(name_en) LIKE '%fish ball%' OR lower(name_en) LIKE '%roe%') AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/usa-beef.jpg' WHERE (lower(name_en) LIKE '%usa beef%' OR lower(name_en) LIKE '%chicken%') AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/prawn-dumplings.jpg' WHERE (lower(name_en) LIKE '%prawn dumpling%' OR lower(name_en) LIKE '%mini dumpling%') AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/sichuan-dumplings.jpg' WHERE lower(name_en) LIKE '%dumpling%' AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/steamed-rice.jpg' WHERE lower(name_en) LIKE '%rice%' AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/apple-tea.jpg' WHERE lower(name_en) LIKE '%apple%' AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/pineapple-tea.jpg' WHERE lower(name_en) LIKE '%pineapple%' AND (image_url IS NULL OR image_url = '');
UPDATE modifier_options SET image_url = '/food-images/honey-lemon.jpg' WHERE lower(name_en) LIKE '%honey lemon%' AND (image_url IS NULL OR image_url = '');
