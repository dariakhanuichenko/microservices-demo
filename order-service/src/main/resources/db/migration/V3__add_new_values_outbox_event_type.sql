UPDATE outbox_order_event SET event_type = 'ORDER_CREATED' WHERE event_type IN ('OrderCreated');

