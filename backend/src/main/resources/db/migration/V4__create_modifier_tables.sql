CREATE TABLE IF NOT EXISTS modifier_groups (
    id UUID PRIMARY KEY,
    name_en VARCHAR(150) NOT NULL,
    name_km VARCHAR(150) NOT NULL,
    min_choice INTEGER NOT NULL DEFAULT 0,
    max_choice INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS modifier_options (
    id UUID PRIMARY KEY,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id),
    name_en VARCHAR(150) NOT NULL,
    name_km VARCHAR(150) NOT NULL,
    image_url VARCHAR(500),
    unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    pack_size VARCHAR(50),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_modifier_options_group_id ON modifier_options (modifier_group_id);

CREATE TABLE IF NOT EXISTS menu_item_modifier_groups (
    id UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL REFERENCES menu_items (id),
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups (id),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_menu_item_modifier_group UNIQUE (menu_item_id, modifier_group_id)
);

CREATE INDEX IF NOT EXISTS idx_menu_item_modifier_groups_menu_item_id ON menu_item_modifier_groups (menu_item_id);
