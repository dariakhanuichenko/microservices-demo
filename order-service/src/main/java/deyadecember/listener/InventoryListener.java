package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.entities.CancellationReason;
import deyadecember.events.model.InventoryRejected;
import deyadecember.events.model.InventoryReserved;
import deyadecember.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryListener {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(InventoryListener.class);
    private final ObjectMapper objectMapper;
    private final OrderService service;

    @KafkaListener(topics = "inventory.reserved")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryTopicSuffix = "-order-retry",
            dltTopicSuffix = "-order.dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenInventoryReserved(@Payload(required = false)String payload, Acknowledgment ack) throws JsonProcessingException {
        if (isNotValidPayload(payload, ack)) {
            return;
        }
        InventoryReserved event = objectMapper.readValue(payload, InventoryReserved.class);
        log.info("💰 InventoryReserved received event: {}", event);
        service.reserveOrder(event.orderId());
        log.info("Order {} marked as reserved", event.orderId());
        ack.acknowledge();
    }

    @KafkaListener(topics = "inventory.rejected")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryTopicSuffix = "-order-retry",
            dltTopicSuffix = "-order.dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenInventoryRejected(@Payload(required = false)String payload, Acknowledgment ack) throws JsonProcessingException {
        if (isNotValidPayload(payload, ack)) {
            return;
        }
        InventoryRejected event = objectMapper.readValue(payload, InventoryRejected.class);
        log.info("💰 InventoryRejected received event: {}", event);
        CancellationReason reason = CancellationReason.fromRejected(event.reason());
        service.cancelOrder(event.orderId(),reason);
        log.info("Order {} marked as canceled because of {}", event.orderId(), reason);
        ack.acknowledge();
    }
    private boolean isNotValidPayload(String payload, Acknowledgment ack) {
        if (payload == null || payload.isBlank()) {
            log.warn("Empty payload in inventory topic, skipping");
            ack.acknowledge();
            return true;
        }
        return false;
    }
}

