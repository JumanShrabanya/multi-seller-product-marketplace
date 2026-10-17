CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE seller (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_seller_status CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED'))
);

CREATE TABLE product (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    unit VARCHAR(100) NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE seller_listing (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product (id),
    seller_id BIGINT NOT NULL REFERENCES seller (id),
    price NUMERIC(12, 2) NOT NULL,
    stock INTEGER NOT NULL,
    min_order_qty INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_seller_listing_seller_product UNIQUE (seller_id, product_id),
    CONSTRAINT chk_seller_listing_price CHECK (price > 0),
    CONSTRAINT chk_seller_listing_stock CHECK (stock >= 0),
    CONSTRAINT chk_seller_listing_min_order_qty CHECK (min_order_qty >= 1),
    CONSTRAINT chk_seller_listing_moq_within_stock CHECK (stock = 0 OR min_order_qty <= stock)
);

CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_brand ON product (brand);
CREATE INDEX idx_product_name_trgm ON product USING GIN (lower(name) gin_trgm_ops);
CREATE INDEX idx_seller_listing_product_id ON seller_listing (product_id);
CREATE INDEX idx_seller_listing_buyable ON seller_listing (product_id) WHERE active AND stock > 0;
CREATE INDEX idx_seller_listing_seller_id ON seller_listing (seller_id);
