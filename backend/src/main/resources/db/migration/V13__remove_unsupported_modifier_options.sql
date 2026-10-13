-- Remove deprecated modifier options: Beef Tendon Ball, Kaixin Fish Ball, Crab Steak, Kiss Hot Dog
DELETE FROM cart_line_modifier_selections
 WHERE modifier_option_id IN (
     SELECT id FROM modifier_options
      WHERE lower(name_en) IN ('beef tendon ball', 'kaixin fish ball', 'crab steak', 'kiss hot dog')
 );

DELETE FROM modifier_options
 WHERE lower(name_en) IN ('beef tendon ball', 'kaixin fish ball', 'crab steak', 'kiss hot dog');
