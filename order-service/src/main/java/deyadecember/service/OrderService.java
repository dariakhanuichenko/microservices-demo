package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.entities.*;
import deyadecember.events.api.CreateOrderRequest;
import deyadecember.events.model.OrderCancelledEvent;
import deyadecember.events.model.OrderCreatedEvent;
import deyadecember.repository.OrderRepository;
import deyadecember.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
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
    private final Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    @Transactional
    public Order createOrder(CreateOrderRequest request) throws JsonProcessingException {
        Order order = createAndSaveOrder(request);
        OutboxEvent outbox = createOutboxEvent(order, EventType.ORDER_CREATED);
        outboxRepo.save(outbox);
        return order;
    }

    @Transactional
    public void reserveOrder(UUID orderId) {
        orderRepository.findById(orderId).ifPresent(i -> {
            if (!i.getReserved()) {
                i.setReserved(true);
                recalculate(i);
            }
        });
    }

    @Transactional(rollbackFor = JsonProcessingException.class)
    public void cancelOrder(UUID orderId, CancellationReason reason) throws JsonProcessingException {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} not found, cancel ignored", orderId);
            return;
        }
        if (OrderStatus.COMPLETED.equals(order.getStatus())) {
            log.info("Skip cancelling order, status already COMPLETED");
            return;
        }
        if (order.getCancellationReason() != null) {
            log.info("Order {} already cancelled ({}), keeping the original reason",
                    orderId, order.getCancellationReason());
            return;
        }
        order.setCancellationReason(reason);
        recalculate(order);
        outboxRepo.save(createOutboxEvent(order, EventType.ORDER_CANCELLED));
    }

    @Transactional
    public void payOrder(UUID orderId) {
        orderRepository.findById(orderId).ifPresent(i -> {
            if (!i.getPaid()) {
                i.setPaid(true);
                recalculate(i);
            }
        });
    }

    private OutboxEvent createOutboxEvent(Order order, EventType eventType) throws JsonProcessingException {
        Object event = null;
        switch (eventType) {
            case ORDER_CREATED ->{
                List<OrderCreatedEvent.Item> eventItems = order.getItems().stream()
                        .map(i -> new OrderCreatedEvent.Item(i.getFlowerId(), i.getQuantity()))
                        .toList();
                event = new OrderCreatedEvent(order.getId().toString(), order.getCustomerId().toString(),
                        eventItems, order.getTotalAmount(), order.getCreatedAt());
            }

            case ORDER_CANCELLED -> {
                event = new OrderCancelledEvent(order.getId(), order.getCustomerId(),
                        order.getTotalAmount(), order.getCancellationReason(), Instant.now());
            }
        }
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(order.getId().toString())
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .processed(false)
                .build();
    }

    private Order createAndSaveOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .createdAt(Instant.now())
                .status(OrderStatus.NEW)
                .customerId(request.customerId())
                .paid(false)
                .reserved(false)
                .build();

        List<OrderItem> items = createOrderItems(order, request.items());
        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotalAmount(total);
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

    private void recalculate(Order o) {
        if (o.getCancellationReason() != null) o.setStatus(OrderStatus.CANCELLED);
        else if (o.getPaid() && o.getReserved()) o.setStatus(OrderStatus.COMPLETED);
        else o.setStatus(OrderStatus.NEW);
    }
}

