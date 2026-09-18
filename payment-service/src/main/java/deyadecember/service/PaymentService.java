package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.entities.EventType;
import deyadecember.entities.OutboxEvent;
import deyadecember.entities.Payment;
import deyadecember.entities.PaymentStatus;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.events.PaymentCompleted;
import deyadecember.events.PaymentFailed;
import deyadecember.events.PaymentRefunded;
import deyadecember.repository.OutboxEventRepository;
import deyadecember.repository.PaymentRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final BigDecimal declineAbove;
    private final PaymentRepository repository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);

    public PaymentService( @Value("${payment.decline-above}") BigDecimal declineAbove,
                          PaymentRepository repository, OutboxEventRepository outboxRepository,
                          ObjectMapper objectMapper) {
        this.declineAbove = declineAbove;
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processPaymentAndSaveOutboxEvent(OrderCreatedEvent event) throws JsonProcessingException {
        Payment payment = createPayment(event);

        if (canProcessPayment(event)) {
            log.info("Payment success for order {}", event.getOrderId());
            payment.setStatus(PaymentStatus.COMPLETED);
            outboxRepository.save(createOutboxEvent(payment, EventType.PAYMENT_COMPLETED));
        } else {
            log.info("Payment failure for order {}", event.getOrderId());
            payment.setStatus(PaymentStatus.FAILED);
            outboxRepository.save(createOutboxEvent(payment, EventType.PAYMENT_FAILED));
        }
        repository.save(payment);
    }

    @Transactional
    public void refund(UUID orderId) throws JsonProcessingException {
        int updated = repository.markRefunded(orderId, Instant.now());
        if (updated == 0) {
            log.info("Nothing to refund for order {} (no payment, or not COMPLETED)", orderId);
            return;
        }
        Payment payment = repository.findByOrderId(orderId).orElseThrow();
        outboxRepository.save(createOutboxEvent(payment, EventType.PAYMENT_REFUNDED));
    }

    private OutboxEvent createOutboxEvent(Payment payment, EventType eventType) throws JsonProcessingException {
        Object event = null;
        switch (eventType) {
            case PAYMENT_COMPLETED -> {
                event = new PaymentCompleted(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        payment.getCreatedAt());
            }
            case PAYMENT_FAILED -> {
                event = new PaymentFailed(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        Instant.now());
            }
            case PAYMENT_REFUNDED -> {
                event = new PaymentRefunded(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        payment.getRefundedAt());
            }
        }
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("OR")
                .aggregateId(payment.getOrderId().toString())
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .processed(false)
                .build();
    }

    private Payment createPayment(OrderCreatedEvent event) {
        UUID paymentId = UUID.nameUUIDFromBytes(
                ("payment:" + event.getOrderId()).getBytes(StandardCharsets.UTF_8));
        return Payment.builder()
                .id(paymentId)
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getTotalAmount())
                .createdAt(Instant.now())
                .build();
    }

    private boolean canProcessPayment(OrderCreatedEvent event) {
        return event.getTotalAmount().compareTo(declineAbove) <= 0;

    }
}
