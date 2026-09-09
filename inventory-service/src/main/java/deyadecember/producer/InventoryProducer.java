package deyadecember.producer;

import deyadecember.events.InventoryReserved;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, InventoryReserved> kafkaTemplate;

    public void send(InventoryReserved event) {
        kafkaTemplate.send("inventory.reserved", event.orderId().toString(), event);
    }
}

