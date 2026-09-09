package deyadecember.listener;

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


    @KafkaListener(topics = "payments.completed")
    public void listen(PaymentCompleted event) {
        statsStore.addPayment(event.customerId(),event.amount());
        log.info("Customer {} now has {}", event.customerId(), statsStore.get(event.customerId()));
    }
}

