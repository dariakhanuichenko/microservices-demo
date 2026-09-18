package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.PaymentCompleted;
import deyadecember.events.PaymentRefunded;
import deyadecember.stats.CustomerStatsStore;
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
public class PaymentListener {
    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentListener.class);
    private final CustomerStatsStore statsStore;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "payments.completed")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryTopicSuffix = "-user-retry",
            dltTopicSuffix = "-user.dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenPaymentCompleted(@Payload(required = false)String payload, Acknowledgment ack) throws JsonProcessingException {
        PaymentCompleted event = objectMapper.readValue(payload, PaymentCompleted.class);
        boolean counted = statsStore.addPayment(event.paymentId(), event.customerId(), event.amount());
        if (counted) {
            log.info("Customer {} now has {}", event.customerId(), statsStore.get(event.customerId()));
        } else {
            log.info("Payment {} already processed, skipped", event.paymentId());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "payments.refunded")
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryTopicSuffix = "-user-retry",
            dltTopicSuffix = "-user.dlq",
            exclude = {JsonProcessingException.class}
    )
    public void listenPaymentRefunded(@Payload(required = false)String payload, Acknowledgment ack) throws JsonProcessingException {
        PaymentRefunded event = objectMapper.readValue(payload, PaymentRefunded.class);
        switch (statsStore.removePayment(event.paymentId(), event.customerId(), event.amount())) {
            case COUNTED   -> log.info("Refund for payment {} subtracted", event.paymentId());
            case DUPLICATE -> log.info("Refund {} already processed, skipped", event.paymentId());
            case ANOMALY   -> throw new IllegalStateException(
                    "Cannot subtract refund for payment " + event.paymentId()
                            + ": stats too low, the original payment may not be counted yet");
        }
        ack.acknowledge();
    }
}

