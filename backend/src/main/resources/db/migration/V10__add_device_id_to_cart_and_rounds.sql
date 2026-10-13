-- Per-device draft carts + one-send-per-device (customer ordering: device identity).
-- Draft cart lines are ephemeral by contract (spec §B core invariant), so existing
-- drafts are discarded rather than backfilled with a fake device id.
DELETE FROM cart_line_items;

ALTER TABLE cart_line_items
    ADD COLUMN device_id UUID NOT NULL;

DROP INDEX IF EXISTS idx_cart_line_items_session_id;
CREATE INDEX IF NOT EXISTS idx_cart_line_items_session_device
    ON cart_line_items (session_id, device_id);

-- Nullable: cashier-submitted rounds and pre-existing rounds have no guest device
ALTER TABLE order_rounds
    ADD COLUMN device_id UUID;

CREATE INDEX IF NOT EXISTS idx_order_rounds_device_id
    ON order_rounds (device_id);
