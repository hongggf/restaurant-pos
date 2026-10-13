CREATE TABLE IF NOT EXISTS cart_line_items (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES table_sessions (id),
    menu_item_id UUID NOT NULL REFERENCES menu_items (id),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    remark VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cart_line_items_session_id ON cart_line_items (session_id);

CREATE TABLE IF NOT EXISTS cart_line_modifier_selections (
    id UUID PRIMARY KEY,
    cart_line_item_id UUID NOT NULL REFERENCES cart_line_items (id) ON DELETE CASCADE,
    modifier_option_id UUID NOT NULL REFERENCES modifier_options (id),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_cart_line_modifier_option UNIQUE (cart_line_item_id, modifier_option_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_line_modifier_selections_line_id
    ON cart_line_modifier_selections (cart_line_item_id);
