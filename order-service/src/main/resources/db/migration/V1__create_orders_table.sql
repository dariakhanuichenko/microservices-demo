CREATE TABLE orders (
                              id UUID PRIMARY KEY,
                              user_id UUID NOT NULL,
                              total_amount NUMERIC NOT NULL,
                              status VARCHAR(50),
                              created_at TIMESTAMP NOT NULL
);
