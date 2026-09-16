package deyadecember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import deyadecember.entities.CancellationReason;
import deyadecember.entities.Order;
import deyadecember.entities.OrderStatus;
import deyadecember.repository.OrderRepository;
import deyadecember.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusTest {

    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock private OrderRepository orderRepository;
    @Mock private OutboxEventRepository outboxRepository;

    private OrderService service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, outboxRepository, new ObjectMapper());

        order = Order.builder()
                .id(ORDER_ID)
                .customerId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(100))
                .createdAt(Instant.now())
                .status(OrderStatus.NEW)
                .paid(false)
                .reserved(false)
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    @Test
    void reservedThenPaidBecomesCompleted() {
        service.reserveOrder(ORDER_ID);

        assertTrue(order.getReserved());
        assertEquals(OrderStatus.NEW, order.getStatus(),
                "one fact out of two is not enough to complete the order");

        service.payOrder(ORDER_ID);

        assertTrue(order.getPaid());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }
    @Test
    void paidThenReservedBecomesCompleted() {
        service.payOrder(ORDER_ID);

        assertTrue(order.getPaid());
        assertEquals(OrderStatus.NEW, order.getStatus(),
                "one fact out of two is not enough to complete the order");

        service.reserveOrder(ORDER_ID);

        assertTrue(order.getReserved());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void completedOrderIsNotCancelledByLateFailure() throws JsonProcessingException {
        service.payOrder(ORDER_ID);
        service.reserveOrder(ORDER_ID);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());

        service.cancelOrder(ORDER_ID, CancellationReason.PAYMENT_FAILED);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertNull(order.getCancellationReason());
    }

    @Test
    void cancelOrderTwiceStoresFirstReason() throws JsonProcessingException {
        service.cancelOrder(ORDER_ID, CancellationReason.OUT_OF_STOCK);

        assertEquals(CancellationReason.OUT_OF_STOCK, order.getCancellationReason());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        service.cancelOrder(ORDER_ID, CancellationReason.PAYMENT_FAILED);

        assertEquals(CancellationReason.OUT_OF_STOCK, order.getCancellationReason());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void payOrderTwiceIsIdempotent() {
        service.payOrder(ORDER_ID);
        service.reserveOrder(ORDER_ID);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertTrue(order.getPaid());

        service.payOrder(ORDER_ID);

        assertTrue(order.getPaid());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }
}