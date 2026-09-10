package deyadecember.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.PaymentCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(PaymentCompleted event) throws JsonProcessingException {
        kafkaTemplate.send("payments.completed",
                event.orderId().toString(),
                objectMapper.writeValueAsString(event));
    }
}

