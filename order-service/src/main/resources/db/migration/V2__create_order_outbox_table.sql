CREATE TABLE outbox_order_event (
                              id UUID PRIMARY KEY,
                              aggregate_type VARCHAR(50),
                              aggregate_id VARCHAR(100),
                              event_type VARCHAR(100),
                              payload JSONB,
                              created_at TIMESTAMP,
                              processed BOOLEAN DEFAULT FALSE
);
