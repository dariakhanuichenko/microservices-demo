package deyadecember.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import deyadecember.events.ReservedItem;
import deyadecember.stock.StockStore;
import deyadecember.stock.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PaymentListenerTest {

    private static final UUID ROSE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final String PAYMENT_FAILED_PAYLOAD = """
            {"paymentId":"33333333-3333-3333-3333-333333333333",\
            "orderId":"55555555-5555-5555-5555-555555555555",\
            "customerId":"99999999-9999-9999-9999-999999999999",\
            "amount":1100,\
            "failedAt":"2026-09-14T10:00:00Z"}""";

    private static final String PAYMENT_COMPLETED_PAYLOAD = """
            {"paymentId":"33333333-3333-3333-3333-333333333333",\
            "orderId":"55555555-5555-5555-5555-555555555555",\
            "customerId":"99999999-9999-9999-9999-999999999999",\
            "amount":900,\
            "paidAt":"2026-09-14T10:00:00Z"}""";


    @Mock
    private Acknowledgment acknowledgment;

    private StockStore stockStore;
    private PaymentListener paymentListener;

    @BeforeEach
    void setUp() {
        stockStore = new StockStore();
        stockStore.setStock(ROSE, new Stock(2, 0));

        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        paymentListener = new PaymentListener(stockStore, mapper);   // без @InjectMocks

    }

    @Test
    void releaseInventoryAfterFailedPaymentEvent() throws Exception {
        stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1)));
        assertEquals(new Stock(1, 1), stockStore.get(ROSE));

        paymentListener.listenFailure(PAYMENT_FAILED_PAYLOAD, acknowledgment);

        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void completedPaymentConfirmsReservation() throws Exception {
        stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1)));

        paymentListener.listenConfirm(PAYMENT_COMPLETED_PAYLOAD, acknowledgment);

        assertEquals(new Stock(1, 0), stockStore.get(ROSE));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void paymentBeforeReserveThrowsAndDoesNotAcknowledge() {
        assertThrows(IllegalStateException.class,
                () -> paymentListener.listenFailure(PAYMENT_FAILED_PAYLOAD, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }
}

