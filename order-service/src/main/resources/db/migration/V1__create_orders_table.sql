-- Початкова схема order-service.

-- ---------------------------------------------------------------------------
-- customers
CREATE TABLE customers (
    id          UUID         PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL
);

-- ---------------------------------------------------------------------------
-- orders
-- ---------------------------------------------------------------------------
CREATE TABLE orders (
    id            UUID           PRIMARY KEY,
    -- FK на customers СВІДОМО немає: покупці переїдуть в user-service, і тоді
    -- зовнішній ключ у цю таблицю стане неможливим. Тримаємо просто значення.
    customer_id   UUID           NOT NULL,
    total_amount  NUMERIC(12, 2) NOT NULL,
    status        VARCHAR(255)   NOT NULL,
    created_at    TIMESTAMP      NOT NULL
);


CREATE INDEX idx_orders_customer_id ON orders (customer_id);

-- ---------------------------------------------------------------------------
-- order_items
-- ---------------------------------------------------------------------------
CREATE TABLE order_items (
    id          UUID           PRIMARY KEY,
    order_id    UUID           NOT NULL,
    flower_id   UUID           NOT NULL,
    quantity    INTEGER        NOT NULL,
    unit_price  NUMERIC(12, 2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE,

    -- одна квітка не може двічі бути окремою позицією того ж замовлення
    CONSTRAINT uq_order_items_order_flower UNIQUE (order_id, flower_id),
    CONSTRAINT chk_order_items_quantity    CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price  CHECK (unit_price >= 0)
);

-- ON DELETE CASCADE відповідає orphanRemoval = true в @OneToMany: видалили
-- замовлення - позиції зникають разом з ним.

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- ---------------------------------------------------------------------------
-- outbox_order_event
-- ---------------------------------------------------------------------------

CREATE TABLE outbox_order_event (
    id              UUID         PRIMARY KEY,
    aggregate_type  VARCHAR(255),
    aggregate_id    VARCHAR(255),
    event_type      VARCHAR(255),
    payload         JSONB,
    created_at      TIMESTAMP,
    processed       BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Точно під запит findTop10ByProcessedFalseOrderByCreatedAt.
-- Частковий індекс (WHERE processed = false) містить лише НЕопубліковані
-- рядки, тому залишається маленьким навіть коли оброблених стане мільйон.
CREATE INDEX idx_outbox_unprocessed
    ON outbox_order_event (created_at)
    WHERE processed = FALSE;

-- ---------------------------------------------------------------------------
-- flower_prices
-- ---------------------------------------------------------------------------
CREATE TABLE flower_prices (
    flower_id   UUID           PRIMARY KEY,
    price       NUMERIC(12, 2) NOT NULL,
    updated_at  TIMESTAMP      NOT NULL,

    CONSTRAINT chk_flower_prices_price CHECK (price >= 0)
);
