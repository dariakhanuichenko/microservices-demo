package deyadecember.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import deyadecember.events.InventoryRejected;
import deyadecember.events.RejectionReason;
import deyadecember.producer.InventoryProducer;
import deyadecember.stock.StockStore;
import deyadecember.stock.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryListenerTest {

    private static final UUID ROSE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNKNOWN_FLOWER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final String OUT_OF_STOCK_PAYLOAD = """
            {"orderId":"55555555-5555-5555-5555-555555555555",\
            "customerId":"99999999-9999-9999-9999-999999999999",\
            "items":[{"flowerId":"11111111-1111-1111-1111-111111111111","quantity":4}],\
            "totalAmount":400,"createdAt":"2026-09-14T10:00:00Z"}""";

    private static final String UNKNOWN_FLOWER_PAYLOAD = """
            {"orderId":"55555555-5555-5555-5555-555555555555",\
            "customerId":"99999999-9999-9999-9999-999999999999",\
            "items":[{"flowerId":"33333333-3333-3333-3333-333333333333","quantity":3}],\
            "totalAmount":300,"createdAt":"2026-09-14T10:00:00Z"}""";

    @Mock
    private InventoryProducer inventoryProducer;
    @Mock
    private Acknowledgment acknowledgment;

    private StockStore stockStore;
    private InventoryListener inventoryListener;

    @BeforeEach
    void setUp() {
        stockStore = new StockStore();
        stockStore.setStock(ROSE, new Stock(2, 0));

        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        inventoryListener = new InventoryListener(stockStore, inventoryProducer, mapper);   // без @InjectMocks

    }

    @Test
    void outOfStockOrderPublishesRejectedEvent() throws Exception {

        inventoryListener.listen(OUT_OF_STOCK_PAYLOAD, acknowledgment);

        ArgumentCaptor<InventoryRejected> captor = ArgumentCaptor.forClass(InventoryRejected.class);
        verify(inventoryProducer).send(eq("inventory.rejected"), eq(ORDER_ID), captor.capture());

        InventoryRejected sent = captor.getValue();
        assertEquals(ORDER_ID, sent.orderId());
        assertEquals(RejectionReason.OUT_OF_STOCK, sent.reason());
        assertEquals(List.of(ROSE), sent.flowerIds());

        verifyNoMoreInteractions(inventoryProducer);
        verify(acknowledgment).acknowledge();
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }

    @Test
    void unknownFlowerOrderPublishesRejectedEvent() throws Exception {

        inventoryListener.listen(UNKNOWN_FLOWER_PAYLOAD, acknowledgment);

        ArgumentCaptor<InventoryRejected> captor = ArgumentCaptor.forClass(InventoryRejected.class);
        verify(inventoryProducer).send(eq("inventory.rejected"), eq(ORDER_ID), captor.capture());

        InventoryRejected sent = captor.getValue();
        assertEquals(ORDER_ID, sent.orderId());
        assertEquals(RejectionReason.UNKNOWN_FLOWER, sent.reason());
        assertEquals(List.of(UNKNOWN_FLOWER), sent.flowerIds());

        verifyNoMoreInteractions(inventoryProducer);
        verify(acknowledgment).acknowledge();
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }
}

