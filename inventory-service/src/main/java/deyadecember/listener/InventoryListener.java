package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.InventoryRejected;
import deyadecember.events.InventoryReserved;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.producer.InventoryProducer;
import deyadecember.stock.StockStore;
import deyadecember.stock.model.Reservation;
import deyadecember.stock.model.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InventoryListener {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(InventoryListener.class);
    private final StockStore stockStore;
    private final InventoryProducer inventoryProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.created")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000),
            dltTopicSuffix = ".dlq",
            exclude = {JsonProcessingException.class, IllegalArgumentException.class}
    )
    public void listen(String payload, Acknowledgment ack) throws JsonProcessingException {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        log.info("💰 OrderCreatedEvent received event: {}", event);

        log.info("Processing inventory for order {}", event.orderId());
        Reservation reservation = stockStore.reserve(event.orderId(), event.items());
        if (!reservation.isReserved()) {
            inventoryProducer.send("inventory.rejected",event.orderId(),
                    new InventoryRejected(event.orderId(), reservation.reason(),
                            reservation.flowerIds(), Instant.now()));
        } else if (reservation.status() == ReservationStatus.RELEASED) {
            log.info("order {} already released, nothing to publish", event.orderId());
        } else {
            inventoryProducer.send("inventory.reserved",event.orderId(),
                    new InventoryReserved(event.orderId(), event.items(), Instant.now()));
        }

        ack.acknowledge();
    }
}

