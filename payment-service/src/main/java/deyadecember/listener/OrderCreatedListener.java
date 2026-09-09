package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.events.PaymentCompleted;
import deyadecember.producer.PaymentProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final PaymentProducer producer;
    private final ObjectMapper objectMapper;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(OrderCreatedListener.class);

    @KafkaListener(topics = "orders.created")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000),
            dltTopicSuffix = ".dlq",
            exclude = {JsonProcessingException.class, IllegalArgumentException.class}
    )
    public void listen(String payload, Acknowledgment ack) throws JsonProcessingException {

        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        log.info("Received order {}", event.getOrderId());

        //todo make payment
        UUID paymentId = UUID.nameUUIDFromBytes(
                event.getOrderId().toString().getBytes(StandardCharsets.UTF_8));
        PaymentCompleted paymentCompleted = new PaymentCompleted(paymentId, event.getOrderId(), event.getCustomerId(), event.getTotalAmount(), Instant.now());
        producer.send(paymentCompleted);
        log.info("Processing payment for order {}", event.getOrderId());

        ack.acknowledge();
    }

    @KafkaListener(
            topics = "orders.created.dlq",
            groupId = "payment-service-dlq"
    )
    public void handleDlq(String payload,
                          @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String errorMessage,
                          @Header(KafkaHeaders.DLT_ORIGINAL_OFFSET) long offset) {
        log.error("EXCEPTION MESSAGE: {}.ORIGINAL OFFSET: {} ", errorMessage, offset);
    }

}

