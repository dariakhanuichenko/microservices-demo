package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.OrderCancelledEvent;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final PaymentService service;
    private final ObjectMapper objectMapper;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(OrderCreatedListener.class);

    @KafkaListener(topics = "orders.created")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000),
            dltTopicSuffix = ".dlq",
            exclude = {JsonProcessingException.class, IllegalArgumentException.class}
    )
    public void listenOrderCreated(@Payload(required = false) String payload, Acknowledgment ack) throws JsonProcessingException {
        if (isNotValidPayload(payload, ack)) {
            return;
        }
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        log.info("Received order {}", event.getOrderId());

        service.processPaymentAndSaveOutboxEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(topics = "orders.cancelled")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000),
            dltTopicSuffix = ".dlq",
            exclude = {JsonProcessingException.class, IllegalArgumentException.class}
    )
    public void listenOrderCancelled(@Payload(required = false) String payload, Acknowledgment ack) throws JsonProcessingException {
        if (isNotValidPayload(payload, ack)) {
            return;
        }
        OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
        log.info("Received order {}", event.orderId());

        service.refund(event.orderId());
        ack.acknowledge();
    }

    @KafkaListener(topicPattern = ".*-orders\\.dlq", groupId = "payment-service-dlq")
    public void handleDlq(@Payload(required = false) String payload,
                          @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String errorMessage,
                          @Header(value = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false) Long offset) {
        log.error("DLQ: {} (original offset {})", errorMessage, offset);
    }

    private boolean isNotValidPayload(String payload, Acknowledgment ack) {
        if (payload == null || payload.isBlank()) {
            log.warn("Empty payload in orders topic, skipping");
            ack.acknowledge();
            return true;
        }
        return false;
    }

}

