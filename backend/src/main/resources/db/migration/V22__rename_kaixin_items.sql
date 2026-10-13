-- Remove Kaixin from modifier options
DELETE FROM modifier_options 
WHERE lower(name_en) = 'kaixin dumplings' OR lower(name_en) = 'kaixin dumpling' OR lower(name_en) = 'dumplings';

-- Rename Kaixin Love Drink to Love Drinks in categories
UPDATE categories 
SET name_en = 'Love Drinks' 
WHERE lower(name_en) = 'kaixin love drink';

-- Remove Kaixin from any other menu items or combos just in case
UPDATE menu_items 
SET name_en = 'Malatang World Football Set' 
WHERE lower(name_en) = 'kaixin world football set';
