CREATE TABLE IF NOT EXISTS order_rounds (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES table_sessions (id),
    round_number INTEGER NOT NULL CHECK (round_number >= 1),
    status VARCHAR(20) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    vat_rate NUMERIC(5, 4) NOT NULL,
    vat_amount NUMERIC(12, 2) NOT NULL,
    grand_total NUMERIC(12, 2) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_order_rounds_session_round UNIQUE (session_id, round_number)
);

CREATE INDEX IF NOT EXISTS idx_order_rounds_session_id ON order_rounds (session_id);
CREATE INDEX IF NOT EXISTS idx_order_rounds_status ON order_rounds (status);
-- sent_at is the FIFO cook queue (cashier spec §3)
CREATE INDEX IF NOT EXISTS idx_order_rounds_sent_at ON order_rounds (sent_at);

-- Immutable send-time snapshots: names and prices are copied so later menu edits
-- never change sent rounds. menu_item_id / modifier_option_id are informational
-- only and must never break the snapshot if the live row disappears.
CREATE TABLE IF NOT EXISTS order_round_line_items (
    id UUID PRIMARY KEY,
    order_round_id UUID NOT NULL REFERENCES order_rounds (id) ON DELETE CASCADE,
    menu_item_id UUID REFERENCES menu_items (id) ON DELETE SET NULL,
    name_en VARCHAR(150) NOT NULL,
    name_km VARCHAR(150) NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    line_total NUMERIC(12, 2) NOT NULL,
    remark VARCHAR(200),
    voided_at TIMESTAMP,
    voided_by UUID REFERENCES users (id),
    void_reason VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_round_line_items_round_id ON order_round_line_items (order_round_id);

CREATE TABLE IF NOT EXISTS order_round_modifier_selections (
    id UUID PRIMARY KEY,
    order_round_line_item_id UUID NOT NULL REFERENCES order_round_line_items (id) ON DELETE CASCADE,
    modifier_option_id UUID REFERENCES modifier_options (id) ON DELETE SET NULL,
    name_en VARCHAR(150) NOT NULL,
    name_km VARCHAR(150) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_round_modifier_selections_line_id
    ON order_round_modifier_selections (order_round_line_item_id);
