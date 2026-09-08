package deyadecember.producer;

import deyadecember.events.PaymentCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, PaymentCompleted> kafkaTemplate;

    public void send(PaymentCompleted event) {
        kafkaTemplate.send("payments.completed", event.orderId().toString(), event);
    }
}

