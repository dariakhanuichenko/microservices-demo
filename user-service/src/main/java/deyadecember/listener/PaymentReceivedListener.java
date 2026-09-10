package deyadecember.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.PaymentCompleted;
import deyadecember.stats.CustomerStatsStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentReceivedListener {
    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentReceivedListener.class);
    private final CustomerStatsStore statsStore;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "payments.completed")
    public void listen(String payload) throws JsonProcessingException {
        PaymentCompleted event = objectMapper.readValue(payload, PaymentCompleted.class);
        statsStore.addPayment(event.customerId(), event.amount());
        log.info("Customer {} now has {}", event.customerId(), statsStore.get(event.customerId()));
    }
}

