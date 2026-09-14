package deyadecember.stock;

import deyadecember.events.RejectionReason;
import deyadecember.events.ReservedItem;
import deyadecember.stock.model.Reservation;
import deyadecember.stock.model.ReservationStatus;
import deyadecember.stock.model.Stock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReserveStockStoreTest extends StockStoreTestBase {

    @Test
    void reservesWhenAllItemsAvailable() {

        assertEquals(ReservationStatus.HELD,
                stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1),
                new ReservedItem(TULIP, 2))).status());

        assertEquals(new Stock(1, 1), stockStore.get(ROSE));
        assertEquals(new Stock(1, 2), stockStore.get(TULIP));
    }

    @Test
    void rejectsWhenOneItemIsShort() {
        Reservation result =stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 4), // not enough flowers
                new ReservedItem(TULIP, 2)));
        assertNull(result.status());

        assertEquals(RejectionReason.OUT_OF_STOCK, result.reason());
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
        assertEquals(new Stock(3, 0), stockStore.get(TULIP));
    }

    @Test
    void duplicateOrderDoesNotReserveTwice() {
        List<ReservedItem> items = List.of(new ReservedItem(ROSE, 1),
                new ReservedItem(TULIP, 1));
        Reservation firstReservation =stockStore.reserve(ORDER_ID, items);
        assertEquals(ReservationStatus.HELD,firstReservation.status());
        assertNull(firstReservation.reason());

        Reservation secondReservation =stockStore.reserve(ORDER_ID, items);

        assertEquals(firstReservation,secondReservation);// duplicate order --> return the same reservation

        assertEquals(new Stock(1, 1), stockStore.get(ROSE));
        assertEquals(new Stock(2, 1), stockStore.get(TULIP));
    }

    @Test
    void rejectsUnknownFlower() {
        Reservation result =stockStore.reserve(ORDER_ID, List.of(new ReservedItem(UNKNOWN_FLOWER, 3)));
        assertNull(result.status());
        assertEquals(RejectionReason.UNKNOWN_FLOWER, result.reason());

        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
        assertEquals(new Stock(3, 0), stockStore.get(TULIP));
    }

    @Test
    void availablePlusReservedNeverChanges() {

        int startSum = getGeneralSum(stockStore.get(ROSE)); // 2 + 0

        List<ReservedItem> items = List.of(new ReservedItem(ROSE, 1));

        assertEquals(ReservationStatus.HELD, stockStore.reserve(ORDER_ID, items).status());
        assertEquals(startSum, getGeneralSum(stockStore.get(ROSE))); // 1 + 1

        assertEquals(ReservationStatus.HELD, stockStore.reserve(UUID.randomUUID(), items).status()); // reserve all left roses
        assertEquals(startSum, getGeneralSum(stockStore.get(ROSE))); // 0 + 2
    }
}
