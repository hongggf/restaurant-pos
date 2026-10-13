-- Correct modifier_options.image_url.
--
-- V11 assigned these with LIKE patterns guarded on `image_url IS NULL`, so the
-- first matching UPDATE won and later, more specific ones never fired:
--   '%apple%'   claimed "Pineapple Lemon Jasmine Tea" before the pineapple rule
--   '%rice%'    claimed "Rice Noodles" before it could get its own image
--   '%chicken%' claimed "Chicken Broth Soup" and "Tender Chicken" for usa-beef
-- It also predates the images added since, where several options shared one file.
--
-- These UPDATEs match on the exact name and are deliberately NOT guarded on
-- NULL, so they overwrite the wrong values V11 left behind.

UPDATE modifier_options SET image_url = '/food-images/tender-chicken.jpg'
 WHERE lower(name_en) IN ('tender chicken');

UPDATE modifier_options SET image_url = '/food-images/usa-beef.jpg'
 WHERE lower(name_en) IN ('usa beef');

UPDATE modifier_options SET image_url = '/food-images/black-chicken.jpg'
 WHERE lower(name_en) IN ('black chicken');

UPDATE modifier_options SET image_url = '/food-images/white-stomach.jpg'
 WHERE lower(name_en) IN ('white stomach');

UPDATE modifier_options SET image_url = '/food-images/juicy-beef-balls.jpg'
 WHERE lower(name_en) IN ('juicy beef ball (2pcs)');

UPDATE modifier_options SET image_url = '/food-images/mini-fish-balls.jpg'
 WHERE lower(name_en) IN ('mini juicy fish ball');

UPDATE modifier_options SET image_url = '/food-images/fish-roe-meatballs.jpg'
 WHERE lower(name_en) IN ('fish roe meatball');

UPDATE modifier_options SET image_url = '/food-images/mini-dumplings.jpg'
 WHERE lower(name_en) IN ('mini dumplings');

UPDATE modifier_options SET image_url = '/food-images/crab-stick.jpg'
 WHERE lower(name_en) IN ('crab stick');

UPDATE modifier_options SET image_url = '/food-images/duck-blood.jpg'
 WHERE lower(name_en) IN ('duck blood');

UPDATE modifier_options SET image_url = '/food-images/potato-noodles.jpg'
 WHERE lower(name_en) IN ('potato noodles');

UPDATE modifier_options SET image_url = '/food-images/bacon.jpg'
 WHERE lower(name_en) IN ('bacon (4pcs)');

UPDATE modifier_options SET image_url = '/food-images/sichuan-dumplings.jpg'
 WHERE lower(name_en) IN ('kaixin dumplings', 'sichuan pork dumplings (5pcs)');

UPDATE modifier_options SET image_url = '/food-images/flower-sausage.jpg'
 WHERE lower(name_en) IN ('pork flower sausage');

UPDATE modifier_options SET image_url = '/food-images/fish-cake.jpg'
 WHERE lower(name_en) IN ('fish cake');

UPDATE modifier_options SET image_url = '/food-images/prawn-dumplings.jpg'
 WHERE lower(name_en) IN ('prawn dumplings');

UPDATE modifier_options SET image_url = '/food-images/fish-roll-meatball.jpg'
 WHERE lower(name_en) IN ('fish roll meatball');

UPDATE modifier_options SET image_url = '/food-images/broccoli.jpg'
 WHERE lower(name_en) IN ('broccoli');

UPDATE modifier_options SET image_url = '/food-images/white-fungus.jpg'
 WHERE lower(name_en) IN ('white fungus');

UPDATE modifier_options SET image_url = '/food-images/black-fungus.jpg'
 WHERE lower(name_en) IN ('black fungus');

UPDATE modifier_options SET image_url = '/food-images/soft-tofu.jpg'
 WHERE lower(name_en) IN ('soft tofu');

UPDATE modifier_options SET image_url = '/food-images/bamboo-shoot.jpg'
 WHERE lower(name_en) IN ('bamboo shoot');

UPDATE modifier_options SET image_url = '/food-images/fried-tofu.jpg'
 WHERE lower(name_en) IN ('deep fried tofu');

UPDATE modifier_options SET image_url = '/food-images/dried-tofu-strips.jpg'
 WHERE lower(name_en) IN ('dried tofu strips');

UPDATE modifier_options SET image_url = '/food-images/needle-mushroom.jpg'
 WHERE lower(name_en) IN ('needle mushroom');

UPDATE modifier_options SET image_url = '/food-images/crab-mushroom.jpg'
 WHERE lower(name_en) IN ('crab mushroom');

UPDATE modifier_options SET image_url = '/food-images/lotus-roots.jpg'
 WHERE lower(name_en) IN ('lotus roots');

UPDATE modifier_options SET image_url = '/food-images/chrysanthemum-greens.jpg'
 WHERE lower(name_en) IN ('tang-o');

UPDATE modifier_options SET image_url = '/food-images/romaine-lettuce.jpg'
 WHERE lower(name_en) IN ('romaine lettuce');

UPDATE modifier_options SET image_url = '/food-images/mee-chiet-noodles.jpg'
 WHERE lower(name_en) IN ('mee chiet noodles');

UPDATE modifier_options SET image_url = '/food-images/handmade-noodles.jpg'
 WHERE lower(name_en) IN ('handmade noodles');

UPDATE modifier_options SET image_url = '/food-images/full-steamed-rice.jpg'
 WHERE lower(name_en) IN ('full steamed rice');

UPDATE modifier_options SET image_url = '/food-images/rice-noodles.jpg'
 WHERE lower(name_en) IN ('rice noodles');

UPDATE modifier_options SET image_url = '/food-images/apple-tea.jpg'
 WHERE lower(name_en) IN ('red apple jasmine tea');

UPDATE modifier_options SET image_url = '/food-images/pineapple-tea.jpg'
 WHERE lower(name_en) IN ('pineapple lemon jasmine tea');

UPDATE modifier_options SET image_url = '/food-images/honey-lemon.jpg'
 WHERE lower(name_en) IN ('honey lemon kiss');

UPDATE modifier_options SET image_url = '/food-images/dry-malatang.jpg'
 WHERE lower(name_en) IN ('dry malatang');

UPDATE modifier_options SET image_url = '/food-images/sichuan-spicy-soup.jpg'
 WHERE lower(name_en) IN ('sichuan spicy soup');

UPDATE modifier_options SET image_url = '/food-images/milky-spicy-soup.jpg'
 WHERE lower(name_en) IN ('milky spicy soup');

UPDATE modifier_options SET image_url = '/food-images/chicken-broth-soup.jpg'
 WHERE lower(name_en) IN ('chicken broth soup');

UPDATE modifier_options SET image_url = '/food-images/mushroom-soup.jpg'
 WHERE lower(name_en) IN ('mushroom soup');

UPDATE modifier_options SET image_url = '/food-images/half-steamed-rice.jpg'
 WHERE lower(name_en) IN ('half steamed rice');

-- Not set here: Kiss Hot Dog, Crab Steak, Beef Tendon Ball and Kaixin Fish
-- Ball. seed.sh now points them at their own images, but those files are not
-- generated yet, so they keep their current ones rather than 404.
