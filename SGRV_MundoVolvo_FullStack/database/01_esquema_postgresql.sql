CREATE TABLE app_users (
 id BIGSERIAL PRIMARY KEY, username VARCHAR(80) NOT NULL, full_name VARCHAR(160) NOT NULL,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL CHECK(role IN ('ADMIN','SELLER','WAREHOUSE')),
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_username_lower ON app_users(lower(username));
CREATE TABLE auth_sessions (
 id BIGSERIAL PRIMARY KEY, token_hash CHAR(64) NOT NULL UNIQUE,
 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_sessions_expires ON auth_sessions(expires_at);
CREATE TABLE brands (id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE);
CREATE TABLE categories (id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE);
CREATE TABLE products (
 id BIGSERIAL PRIMARY KEY, code VARCHAR(90) NOT NULL, name VARCHAR(220) NOT NULL,
 description TEXT, brand_id BIGINT NOT NULL REFERENCES brands(id), category_id BIGINT NOT NULL REFERENCES categories(id),
 stock NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(stock >= 0), min_stock NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(min_stock >= 0),
 purchase_cost NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK(purchase_cost >= 0), sale_price NUMERIC(14,2) NOT NULL CHECK(sale_price >= 0),
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_products_name ON products(lower(name));
CREATE TABLE customers (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, document VARCHAR(30), phone VARCHAR(40), email VARCHAR(140), address TEXT,
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE suppliers (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, tax_id VARCHAR(30), phone VARCHAR(40), email VARCHAR(140), address TEXT,
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE quotes (
 id BIGSERIAL PRIMARY KEY, quote_number VARCHAR(32) UNIQUE, customer_id BIGINT NOT NULL REFERENCES customers(id),
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','SENT','APPROVED','REJECTED','CONVERTED')),
 discount_pct NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK(discount_pct BETWEEN 0 AND 100),
 subtotal NUMERIC(14,2) NOT NULL DEFAULT 0, discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
 tax_rate NUMERIC(5,4) NOT NULL DEFAULT 0.1800, tax NUMERIC(14,2) NOT NULL DEFAULT 0,
 total NUMERIC(14,2) NOT NULL DEFAULT 0, notes TEXT, converted_sale_id BIGINT UNIQUE,
 created_by BIGINT NOT NULL REFERENCES app_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE quote_items (
 id BIGSERIAL PRIMARY KEY, quote_id BIGINT NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
 product_id BIGINT NOT NULL REFERENCES products(id), product_code VARCHAR(90) NOT NULL,
 product_name VARCHAR(220) NOT NULL, quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0),
 unit_price NUMERIC(14,2) NOT NULL CHECK(unit_price >= 0), line_total NUMERIC(14,2) NOT NULL,
 UNIQUE(quote_id, product_id)
);
CREATE TABLE sales (
 id BIGSERIAL PRIMARY KEY, sale_number VARCHAR(32) UNIQUE, customer_id BIGINT NOT NULL REFERENCES customers(id),
 quote_id BIGINT UNIQUE REFERENCES quotes(id), status VARCHAR(12) NOT NULL DEFAULT 'CONFIRMED' CHECK(status IN ('CONFIRMED','CANCELLED')),
 subtotal NUMERIC(14,2) NOT NULL, discount_pct NUMERIC(6,2) NOT NULL DEFAULT 0,
 discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0, tax NUMERIC(14,2) NOT NULL, total NUMERIC(14,2) NOT NULL,
 created_by BIGINT NOT NULL REFERENCES app_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), cancelled_at TIMESTAMPTZ
);
ALTER TABLE quotes ADD CONSTRAINT fk_quotes_converted_sale FOREIGN KEY(converted_sale_id) REFERENCES sales(id);
CREATE TABLE sale_items (
 id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
 product_id BIGINT NOT NULL REFERENCES products(id), product_code VARCHAR(90) NOT NULL, product_name VARCHAR(220) NOT NULL,
 quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0), unit_price NUMERIC(14,2) NOT NULL,
 line_total NUMERIC(14,2) NOT NULL, UNIQUE(sale_id,product_id)
);
CREATE TABLE purchases (
 id BIGSERIAL PRIMARY KEY, purchase_number VARCHAR(32) UNIQUE, supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
 status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PARTIAL','RECEIVED')),
 total NUMERIC(14,2) NOT NULL DEFAULT 0, notes TEXT,
 created_by BIGINT NOT NULL REFERENCES app_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE purchase_items (
 id BIGSERIAL PRIMARY KEY, purchase_id BIGINT NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
 product_id BIGINT NOT NULL REFERENCES products(id), quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0),
 received_qty NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(received_qty >= 0 AND received_qty <= quantity),
 unit_cost NUMERIC(14,2) NOT NULL CHECK(unit_cost >= 0), line_total NUMERIC(14,2) NOT NULL,
 UNIQUE(purchase_id,product_id)
);
CREATE TABLE purchase_receipts (
 id BIGSERIAL PRIMARY KEY, purchase_id BIGINT NOT NULL REFERENCES purchases(id), created_by BIGINT NOT NULL REFERENCES app_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE purchase_receipt_items (
 id BIGSERIAL PRIMARY KEY, receipt_id BIGINT NOT NULL REFERENCES purchase_receipts(id),
 purchase_item_id BIGINT NOT NULL REFERENCES purchase_items(id), quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0),
 UNIQUE(receipt_id,purchase_item_id)
);
CREATE TABLE stock_movements (
 id BIGSERIAL PRIMARY KEY, product_id BIGINT NOT NULL REFERENCES products(id),
 direction VARCHAR(5) NOT NULL CHECK(direction IN ('IN','OUT')),
 quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0), balance_after NUMERIC(14,3) NOT NULL CHECK(balance_after >= 0),
 reference_type VARCHAR(30) NOT NULL, reference_id BIGINT NOT NULL,
 note TEXT, created_by BIGINT NOT NULL REFERENCES app_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(product_id,direction,reference_type,reference_id)
);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id, created_at DESC);
CREATE TABLE audit_logs (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT REFERENCES app_users(id), action VARCHAR(100) NOT NULL,
 entity_type VARCHAR(40) NOT NULL, entity_id BIGINT, details TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE app_settings (key VARCHAR(80) PRIMARY KEY, value VARCHAR(400) NOT NULL);
INSERT INTO app_settings(key,value) VALUES
 ('tax_rate','0.18'),('prevent_negative_stock','true'),('electronic_invoicing','NOT_IMPLEMENTED'),('receipt_printing','PRINT_VIEW_ONLY');
