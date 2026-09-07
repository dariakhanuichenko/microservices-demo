package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.dto.OrderCreatedEvent;
import deyadecember.producer.PaymentProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final PaymentProducer producer;
    private ObjectMapper objectMapper = new ObjectMapper();
    private final Logger log = org.slf4j.LoggerFactory.getLogger(OrderCreatedListener.class);

    @KafkaListener(topics = "orders.created")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000),
            dltTopicSuffix = ".dlq"
    )
    public void listen(String payload, Acknowledgment ack) throws JsonProcessingException {

        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            log.info("Received order {}", event.getOrderId());

            if (event.getAmount() <= 0) {
                throw new IllegalArgumentException("Amount cannot be negative");

            }
            producer.send(event);
            log.info("Processing payment for order {}", event.getOrderId());

            ack.acknowledge();

        } catch (IllegalArgumentException e) {
            log.error("Invalid event {}", payload, e);
            throw e;

        } catch (JsonProcessingException e) {
            log.error("Invalid json {}", payload, e);
            throw e;

        } catch (Exception e) {
            log.error("Temporary error", e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "orders.created.dlq",
            groupId = "payment-service-dlq"
    )
    public void handleDlq(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            log.error("Message in DLQ: {}", event);
        } catch (JsonProcessingException e) {
            log.error("Invalid json in DLQ: {}", payload, e);
        }
    }

}

