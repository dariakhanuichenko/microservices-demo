package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.PaymentCompleted;
import deyadecember.events.PaymentFailed;
import deyadecember.stock.StockStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentListener {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentListener.class);
    private final StockStore stockStore;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payments.completed")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenConfirm(String payload, Acknowledgment ack) throws JsonProcessingException {
        PaymentCompleted event = objectMapper.readValue(payload, PaymentCompleted.class);
        log.info("💰 PaymentCompleted received event: {}", event);

        log.info("Confirm inventory for order {}", event.orderId());
        stockStore.confirm(event.orderId());
        ack.acknowledge();
    }

    @KafkaListener(topics = "payments.failed")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenFailure(String payload, Acknowledgment ack) throws JsonProcessingException {
        PaymentFailed event = objectMapper.readValue(payload, PaymentFailed.class);
        log.info("💰 PaymentFailed received event: {}", event);

        log.info("Release inventory for order {}", event.orderId());
        stockStore.release(event.orderId());
        ack.acknowledge();
    }
}

