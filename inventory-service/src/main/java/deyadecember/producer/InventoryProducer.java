package deyadecember.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(InventoryProducer.class);

    public void send(String topic, UUID key, Object event) throws JsonProcessingException {
        log.info("Sending {} event for order: {}",topic, key);
        try {
            kafkaTemplate.send(topic,
                            key.toString(),
                            objectMapper.writeValueAsString(event))
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while publishing inventory", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("failed to publish inventory", e);
        }
    }
}
