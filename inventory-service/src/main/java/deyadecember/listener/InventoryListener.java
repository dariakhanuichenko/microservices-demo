package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.InventoryReserved;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.producer.InventoryProducer;
import deyadecember.stock.StockStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InventoryListener {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(InventoryListener.class);
    private final StockStore stockStore;
    private final InventoryProducer producer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.created")
    public void listen(String payload) throws JsonProcessingException {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        log.info("💰 OrderCreatedEvent received event: {}", event);

        if (stockStore.reserve(event.getOrderId(), event.getItems())) {
            InventoryReserved inventoryReserved = new InventoryReserved(event.getOrderId(), event.getItems(), Instant.now());
            producer.send(inventoryReserved);
        }

        log.info("Processing inventory for order {}", event.getOrderId());
    }
}

