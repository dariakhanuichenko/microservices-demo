package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.dto.OrderCreatedEvent;
import deyadecember.entities.Order;
import deyadecember.entities.OrderStatus;
import deyadecember.entities.OutboxEvent;
import deyadecember.repository.OrderRepository;
import deyadecember.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createOrder(Double amount) throws JsonProcessingException {
        Order order = createAndSaveOrder(amount);

        OutboxEvent outbox = createOutboxEvent(order);

        outboxRepo.save(outbox);
    }

    private Order createAndSaveOrder(double amount) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(amount))
                .createdAt(Instant.now())
                .status(OrderStatus.NEW)
                .userId(UUID.randomUUID()).build();

        return orderRepository.save(order);
    }

    private OutboxEvent createOutboxEvent(Order order) throws JsonProcessingException {

        OrderCreatedEvent event =
                new OrderCreatedEvent(order.getId().toString(), order.getTotalAmount().doubleValue());

        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(order.getId().toString())
                .eventType("OrderCreated")
                .payload(objectMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .processed(false)
                .build();
    }
}

