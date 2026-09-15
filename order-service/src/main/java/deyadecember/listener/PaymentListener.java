package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.entities.CancellationReason;
import deyadecember.events.PaymentCompleted;
import deyadecember.events.PaymentFailed;
import deyadecember.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentListener {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentListener.class);
    private final ObjectMapper objectMapper;
    private final OrderService service;

    @KafkaListener(topics = "payments.completed")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryTopicSuffix = "-order-retry",
            dltTopicSuffix = "-order.dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenPaymentCompleted(@Payload(required = false)String payload, Acknowledgment ack) throws JsonProcessingException {

        if (isNotValidPayload(payload, ack)) {
            return;
        }

        PaymentCompleted event = objectMapper.readValue(payload, PaymentCompleted.class);
        log.info("💰 PaymentCompleted received event: {}", event);
        service.payOrder(event.orderId());
        log.info("Order {} marked as paid", event.orderId());
        ack.acknowledge();
    }

    @KafkaListener(topics = "payments.failed")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryTopicSuffix = "-order-retry",
            dltTopicSuffix = "-order.dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenPaymentFailed(@Payload(required = false)String payload, Acknowledgment ack) throws JsonProcessingException {

        if (isNotValidPayload(payload, ack)) {
            return;
        }

        PaymentFailed event = objectMapper.readValue(payload, PaymentFailed.class);
        log.info("💰 PaymentFailed received event: {}", event);
        service.cancelOrder(event.orderId(), CancellationReason.PAYMENT_FAILED);
        log.info("Order {} marked as canceled because PAYMENT_FAILED", event.orderId());
        ack.acknowledge();
    }

    @KafkaListener(topicPattern = ".*-order\\.dlq", groupId = "order-service-dlq")
    public void handleDlq(@Payload(required = false) String payload,
                          @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String errorMessage,
                          @Header(value = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false) Long offset) {
        log.error("DLQ: {} (original offset {})", errorMessage, offset);
    }

    private boolean isNotValidPayload(String payload, Acknowledgment ack) {
        if (payload == null || payload.isBlank()) {
            log.warn("Empty payload in payments topic, skipping");
            ack.acknowledge();
            return true;
        }
        return false;
    }

}

