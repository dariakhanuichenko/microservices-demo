package deyadecember.listener;

import deyadecember.dto.OrderCreatedEvent;
import deyadecember.producer.PaymentProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final PaymentProducer producer;


    @KafkaListener(topics = "orders.created")
    public void listen(OrderCreatedEvent event) {

        System.out.println("💰 PaymentService received event: " + event);
        producer.send( event);
    }
}

