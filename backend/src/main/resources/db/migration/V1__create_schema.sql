-- =====================================================================
-- ShopSphere AI - V1 schema (Flyway). Never edit an applied migration:
-- add a new V<n>__description.sql instead.
-- =====================================================================

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    phone         VARCHAR(20),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    sku            VARCHAR(40)    NOT NULL UNIQUE,
    name           VARCHAR(150)   NOT NULL,
    brand          VARCHAR(80),
    description    TEXT,
    price          NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    mrp            NUMERIC(12, 2) CHECK (mrp IS NULL OR mrp > 0),
    stock_quantity INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    image_url      VARCHAR(500),
    ram_gb         INT CHECK (ram_gb IS NULL OR ram_gb >= 0),
    storage_gb     INT CHECK (storage_gb IS NULL OR storage_gb >= 0),
    processor      VARCHAR(120),
    display        VARCHAR(120),
    badge          VARCHAR(40),
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    category_id    BIGINT         NOT NULL REFERENCES categories (id),
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_price ON products (price);
CREATE INDEX ix_products_active ON products (active);

CREATE TABLE cart (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT NOT NULL REFERENCES cart (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id),
    quantity   INT    NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    order_number     VARCHAR(30)    NOT NULL UNIQUE,
    user_id          BIGINT         NOT NULL REFERENCES users (id),
    status           VARCHAR(20)    NOT NULL CHECK (status IN ('PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    payment_method   VARCHAR(30)    NOT NULL CHECK (payment_method IN ('CASH_ON_DELIVERY', 'DEMO_CARD')),
    total_amount     NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    shipping_name    VARCHAR(120)   NOT NULL,
    shipping_phone   VARCHAR(20)    NOT NULL,
    shipping_address VARCHAR(300)   NOT NULL,
    shipping_city    VARCHAR(80)    NOT NULL,
    shipping_state   VARCHAR(80)    NOT NULL,
    shipping_pin     VARCHAR(10)    NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_created ON orders (created_at DESC);
CREATE INDEX ix_orders_status ON orders (status);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         REFERENCES products (id) ON DELETE SET NULL,
    product_name VARCHAR(150)   NOT NULL,
    image_url    VARCHAR(500),
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(12, 2) NOT NULL
);
CREATE INDEX ix_order_items_order ON order_items (order_id);
