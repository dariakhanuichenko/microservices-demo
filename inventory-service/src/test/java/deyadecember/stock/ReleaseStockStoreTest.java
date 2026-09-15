package deyadecember.stock;

import deyadecember.events.RejectionReason;
import deyadecember.events.ReservedItem;
import deyadecember.stock.model.Reservation;
import deyadecember.stock.model.ReservationStatus;
import deyadecember.stock.model.Stock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseStockStoreTest extends StockStoreTestBase {

    @Test
    void releaseTwiceDoesNotReturnStockTwice() {

        assertEquals(ReservationStatus.HELD,
                stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1))).status());
        assertEquals(new Stock(1, 1), stockStore.get(ROSE));

        stockStore.release(ORDER_ID);
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));

        stockStore.release(ORDER_ID);
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }

    @Test
    void releaseRejectedOrderIsIgnored() {
        Reservation result = stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 4), // not enough flowers
                new ReservedItem(TULIP, 2)));
        assertNull(result.status()); //rejected

        assertEquals(RejectionReason.OUT_OF_STOCK, result.reason());

        stockStore.release(ORDER_ID);
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }

    @Test
    void releaseBeforeReserveThrowsSoMessageIsRetried() {
        assertThrows(IllegalStateException.class,
                () -> stockStore.release(ORDER_ID));
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
        assertEquals(new Stock(3, 0), stockStore.get(TULIP));
    }
}
