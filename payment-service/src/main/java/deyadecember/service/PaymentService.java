package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.events.PaymentCompleted;
import deyadecember.events.PaymentFailed;
import deyadecember.producer.PaymentProducer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PaymentService {

    private final BigDecimal declineAbove;
    private final PaymentProducer producer;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(PaymentProducer producer, @Value("${payment.decline-above}") BigDecimal declineAbove) {
        this.declineAbove = declineAbove;
        this.producer = producer;
    }

    public void publishEvents(OrderCreatedEvent event) throws JsonProcessingException {
        UUID paymentId = UUID.nameUUIDFromBytes(
                ("payment:" + event.getOrderId()).getBytes(StandardCharsets.UTF_8));

        if (processPayment(event)) {
            log.info("Payment success for order {}", event.getOrderId());
            PaymentCompleted paymentCompleted = new PaymentCompleted(paymentId, event.getOrderId(), event.getCustomerId(), event.getTotalAmount(), event.getCreatedAt());
            producer.send("payments.completed", event.getOrderId(), paymentCompleted);
        } else {
            log.info("Payment failure for order {}", event.getOrderId());
            PaymentFailed paymentFailed = new PaymentFailed(paymentId, event.getOrderId(), event.getCustomerId(), event.getTotalAmount(), event.getCreatedAt());
            producer.send("payments.failed", event.getOrderId(), paymentFailed);
        }
    }

    public boolean processPayment(OrderCreatedEvent event) {
        return declineAbove.compareTo(event.getTotalAmount()) >= 1;

    }
}
