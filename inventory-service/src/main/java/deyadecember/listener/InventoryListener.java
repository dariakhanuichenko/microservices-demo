package deyadecember.listener;

import deyadecember.events.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryListener {

    @KafkaListener(topics = "user.updated")
    public void listen(OrderCreatedEvent event) {
        System.out.println("💰 Inventory received event: " + event);
    }
}

