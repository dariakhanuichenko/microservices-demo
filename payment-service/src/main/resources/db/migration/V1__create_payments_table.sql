CREATE TABLE payments (
                          id          UUID PRIMARY KEY,
                          order_id    UUID NOT NULL UNIQUE,   -- ← ось це і є ідемпотентність
                          customer_id UUID NOT NULL,
                          amount      NUMERIC(12,2) NOT NULL,
                          status      VARCHAR(20) NOT NULL,   -- COMPLETED | FAILED | REFUNDED
                          created_at  TIMESTAMP NOT NULL,
                          refunded_at  TIMESTAMP
);
---------------------------------------------------------------
CREATE TABLE outbox_payment_event (
                                    id              UUID         PRIMARY KEY,
                                    aggregate_type  VARCHAR(255),
                                    aggregate_id    VARCHAR(255),
                                    event_type      VARCHAR(255),
                                    payload         JSONB,
                                    created_at      TIMESTAMP,
                                    processed       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unprocessed
    ON outbox_payment_event (created_at)
    WHERE processed = FALSE;