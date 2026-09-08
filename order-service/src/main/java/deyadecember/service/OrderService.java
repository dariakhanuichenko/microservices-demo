package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.events.api.CreateOrderRequest;
import deyadecember.entities.Order;
import deyadecember.entities.OrderItem;
import deyadecember.entities.OrderStatus;
import deyadecember.entities.OutboxEvent;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.repository.OrderRepository;
import deyadecember.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(CreateOrderRequest request) throws JsonProcessingException {
        Order order = createAndSaveOrder(request);

        OutboxEvent outbox = createOutboxEvent(order);

        outboxRepo.save(outbox);
        return order;
    }

    private Order createAndSaveOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                //todo: rewrite  using price*quantity
                .totalAmount(BigDecimal.valueOf(10))
                .createdAt(Instant.now())
                .status(OrderStatus.NEW)
                .customerId(request.customerId()).build();

        order.setItems(createOrderItems(order, request.items()));

        return orderRepository.save(order);
    }

    private List<OrderItem> createOrderItems(Order order, List<CreateOrderRequest.Item> items) {
        return items
                .stream()
                .map(dto -> OrderItem.builder()
                        .id(UUID.randomUUID())
                        .flowerId(dto.flowerId())
                        .quantity(dto.quantity())
                        .unitPrice(BigDecimal.valueOf(100)) // todo rewrite
                        .order(order)
                        .build()).toList();
    }

    private OutboxEvent createOutboxEvent(Order order) throws JsonProcessingException {

        List<OrderCreatedEvent.Item> eventItems = order.getItems().stream()
                .map(i -> new OrderCreatedEvent.Item(i.getFlowerId(), i.getQuantity()))
                .toList();

        OrderCreatedEvent event =
                new OrderCreatedEvent(order.getId().toString(), order.getCustomerId().toString(), eventItems, order.getTotalAmount(), order.getCreatedAt());

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

