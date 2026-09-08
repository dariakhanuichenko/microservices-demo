package deyadecember.producer;

import deyadecember.entities.OutboxEvent;
import deyadecember.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderEventProducer {
    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events =
                repo.findTop10ByProcessedFalseOrderByCreatedAt();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        "orders.created",
                        event.getAggregateId(),
                        event.getPayload()
                ).get();

                event.setProcessed(true);
            } catch (Exception e) {
                log.error("Failed to send event {}: {}", event.getId(), e.getMessage());

            }
        }
        repo.saveAll(events);

    }
}

