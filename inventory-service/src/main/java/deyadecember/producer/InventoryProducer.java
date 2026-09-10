package deyadecember.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.InventoryReserved;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(InventoryReserved event) throws JsonProcessingException {
        kafkaTemplate.send("inventory.reserved",
                event.orderId().toString(),
                objectMapper.writeValueAsString(event));
    }
}

