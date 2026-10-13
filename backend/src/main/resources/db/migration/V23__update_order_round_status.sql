ALTER TABLE order_rounds ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE order_rounds ADD COLUMN fulfillment_status VARCHAR(20) NOT NULL DEFAULT 'NEW';

UPDATE order_rounds SET fulfillment_status = 'NEW', payment_status = 'UNPAID' WHERE status = 'SENT';
UPDATE order_rounds SET fulfillment_status = 'READY', payment_status = 'UNPAID' WHERE status = 'READY';
UPDATE order_rounds SET fulfillment_status = 'SERVED', payment_status = 'PAID' WHERE status = 'COMPLETED';
UPDATE order_rounds SET fulfillment_status = 'CANCELLED', payment_status = 'UNPAID' WHERE status = 'CANCELLED';

ALTER TABLE order_rounds DROP COLUMN status;

ALTER TABLE menu_items ADD COLUMN station VARCHAR(20) NOT NULL DEFAULT 'KITCHEN';
ALTER TABLE order_round_line_items ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SENT';
ALTER TABLE order_round_line_items ADD COLUMN station VARCHAR(20) NOT NULL DEFAULT 'KITCHEN';
