package deyadecember.listener;

import deyadecember.dto.OrderCreatedEvent;
import deyadecember.producer.UserProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentReceivedListener {

    private final UserProducer producer;


    @KafkaListener(topics = "payment.completed")
    public void listen(OrderCreatedEvent event) {
        System.out.println("💰 UserService received event: " + event);
        producer.send(event);
    }
}

