package deyadecember.producer;

import deyadecember.entities.OutboxEvent;
import deyadecember.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository repository;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentProducer.class);


    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events =
                repository.findTop10ByProcessedFalseOrderByCreatedAt();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        event.getEventType().topic(),
                        event.getAggregateId(),
                        event.getPayload()
                ).get(5, TimeUnit.SECONDS);

                event.setProcessed(true);
            } catch (Exception e) {
                log.error("Failed to send event {}: {}", event.getId(), e.getMessage());
            }
        }
        repository.saveAll(events);
    }
}

