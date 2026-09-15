package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.entities.*;
import deyadecember.events.OrderCreatedEvent;
import deyadecember.events.api.CreateOrderRequest;
import deyadecember.repository.OrderRepository;
import deyadecember.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

        OutboxEvent outbox = createOutboxEvent(order);

        outboxRepo.save(outbox);
        return order;
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


    public Optional<Order> getOrderById(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public void reserveOrder(UUID orderId) {
        getOrderById(orderId).ifPresent(i -> {
            if (!i.getReserved()) {
                i.setReserved(true);
                recalculate(i);
            }
        });
    }

    @Transactional
    public void cancelOrder(UUID orderId, CancellationReason reason ) {
        getOrderById(orderId).ifPresent(i -> {
            if(OrderStatus.COMPLETED.equals(i.getStatus())){
                log.info("Skip cancelling order, status already COMPLETED");
                return;
            }
            if (i.getCancellationReason() != null) {
                log.info("Order {} already cancelled ({}), keeping the original reason",
                        orderId, i.getCancellationReason());
                return;
            }
            i.setCancellationReason(reason);
            recalculate(i);
        });
    }

    @Transactional
    public void payOrder(UUID orderId) {
        getOrderById(orderId).ifPresent(i -> {
            if (!i.getPaid()) {
                i.setPaid(true);
                recalculate(i);
            }
        });
    }
    private void recalculate(Order o) {
        if (o.getCancellationReason() != null)      o.setStatus(OrderStatus.CANCELLED);
        else if (o.getPaid() && o.getReserved())    o.setStatus(OrderStatus.COMPLETED);
        else                                        o.setStatus(OrderStatus.NEW);
    }


}

