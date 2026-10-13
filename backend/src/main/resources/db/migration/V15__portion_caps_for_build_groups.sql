-- Cap how much food a built bowl can hold, per group.
--
-- max_choice is now read as a portion count (see CartValidationServiceImpl):
-- 6 beef + 4 chicken fills Meat exactly, and each group has its own budget, so
-- a full Meat group never blocks Veggie or Meat Ball.
--
-- The previous values were unreachable under that reading — Meat allowed 20
-- across 4 options, Meat Ball 30 across 13 — so nothing capped a bowl at all.
UPDATE modifier_groups SET max_choice = 10 WHERE lower(name_en) = 'meat';
UPDATE modifier_groups SET max_choice = 15 WHERE lower(name_en) = 'meat ball';
UPDATE modifier_groups SET max_choice = 10 WHERE lower(name_en) = 'veggie';
UPDATE modifier_groups SET max_choice = 5  WHERE lower(name_en) = 'noodles & rice';
