CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    name_en VARCHAR(150) NOT NULL,
    name_km VARCHAR(150) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS menu_items (
    id UUID PRIMARY KEY,
    name_en VARCHAR(150) NOT NULL,
    name_km VARCHAR(150) NOT NULL,
    description_en TEXT,
    description_km TEXT,
    price NUMERIC(12, 2) NOT NULL,
    currency_id UUID NOT NULL REFERENCES currencies (id),
    image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    category_id UUID NOT NULL REFERENCES categories (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_menu_items_category_id ON menu_items (category_id);
