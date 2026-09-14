package deyadecember.stock;

import deyadecember.events.RejectionReason;
import deyadecember.events.ReservedItem;
import deyadecember.stock.model.Reservation;
import deyadecember.stock.model.ReservationStatus;
import deyadecember.stock.model.Stock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmStockStoreTest extends StockStoreTestBase {
    @Test
    void confirmReducesReservedAndTotal() {

        assertEquals(ReservationStatus.HELD,
                stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1))).status());
        assertEquals(new Stock(1, 1), stockStore.get(ROSE));

        stockStore.confirm(ORDER_ID);
        assertEquals(new Stock(1, 0), stockStore.get(ROSE));
    }

    @Test
    void confirmTwiceChangesNothing() {

        assertEquals(ReservationStatus.HELD,
                stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1))).status());
        assertEquals(new Stock(1, 1), stockStore.get(ROSE));

        stockStore.confirm(ORDER_ID);
        assertEquals(new Stock(1, 0), stockStore.get(ROSE));

        stockStore.confirm(ORDER_ID);
        assertEquals(new Stock(1, 0), stockStore.get(ROSE));
    }

    @Test
    void confirmAfterReleaseIsIgnored() {

        assertEquals(ReservationStatus.HELD,
                stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1))).status());
        assertEquals(new Stock(1, 1), stockStore.get(ROSE));

        stockStore.release(ORDER_ID);
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));

        stockStore.confirm(ORDER_ID);
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }

    @Test
    void confirmRejectedOrderIsIgnored() {

        Reservation result = stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 4), // not enough flowers
                new ReservedItem(TULIP, 2)));
        assertNull(result.status()); //rejected

        assertEquals(RejectionReason.OUT_OF_STOCK, result.reason());

        stockStore.confirm(ORDER_ID);
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
    }

    @Test
    void confirmBeforeReserveThrowsSoMessageIsRetried() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> stockStore.confirm(ORDER_ID));
        assertTrue(e.getMessage().contains(ORDER_ID.toString()));
    }

    @Test
    void replayAfterConfirmStillReportsSuccess() {
        List<ReservedItem> items = List.of(new ReservedItem(ROSE, 1));

        assertTrue(stockStore.reserve(ORDER_ID, items).isReserved());
        stockStore.confirm(ORDER_ID);

        Reservation replay = stockStore.reserve(ORDER_ID, items);

        assertTrue(replay.isReserved());
        assertEquals(ReservationStatus.CONFIRMED, replay.status());
        assertEquals(new Stock(1, 0), stockStore.get(ROSE));
    }


}
