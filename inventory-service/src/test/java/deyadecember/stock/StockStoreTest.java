package deyadecember.stock;

import deyadecember.events.ReservedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockStoreTest {

    private static final UUID ROSE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TULIP = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNKNOWN_FLOWER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private StockStore stockStore;

    @BeforeEach
    void setUp() {
        stockStore = new StockStore();
        stockStore.setStock(ROSE, new Stock(2, 0));
        stockStore.setStock(TULIP, new Stock(3, 0));
    }

    @Test
    void reservesWhenAllItemsAvailable() {

        assertTrue(stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 1),
                new ReservedItem(TULIP, 2))));

        assertEquals(new Stock(1, 1), stockStore.get(ROSE));
        assertEquals(new Stock(1, 2), stockStore.get(TULIP));
    }

    @Test
    void rejectsWhenOneItemIsShort() {
        assertFalse(stockStore.reserve(ORDER_ID, List.of(new ReservedItem(ROSE, 4), // not enough flowers
                new ReservedItem(TULIP, 2))));

        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
        assertEquals(new Stock(3, 0), stockStore.get(TULIP));
    }

    @Test
    void duplicateOrderDoesNotReserveTwice() {
        List<ReservedItem> items = List.of(new ReservedItem(ROSE, 1),
                new ReservedItem(TULIP, 1));
        assertTrue(stockStore.reserve(ORDER_ID, items));

        assertFalse(stockStore.reserve(ORDER_ID, items));// duplicate order
        assertEquals(new Stock(1, 1), stockStore.get(ROSE));
        assertEquals(new Stock(2, 1), stockStore.get(TULIP));
    }

    @Test
    void rejectsUnknownFlower() {
        assertFalse(stockStore.reserve(ORDER_ID, List.of(new ReservedItem(UNKNOWN_FLOWER, 3))));
        assertEquals(new Stock(2, 0), stockStore.get(ROSE));
        assertEquals(new Stock(3, 0), stockStore.get(TULIP));
    }

    @Test
    void availablePlusReservedNeverChanges() {

        int startSum = getGeneralSum(stockStore.get(ROSE)); // 2 + 0

        List<ReservedItem> items = List.of(new ReservedItem(ROSE, 1));

        assertTrue(stockStore.reserve(ORDER_ID, items));
        assertEquals(startSum, getGeneralSum(stockStore.get(ROSE))); // 1 + 1

        assertTrue(stockStore.reserve(UUID.randomUUID(), items)); // reserve all left roses
        assertEquals(startSum, getGeneralSum(stockStore.get(ROSE))); // 0 + 2
    }

    private int getGeneralSum(Stock stock) {
        return stock.available() + stock.reserved();
    }
}
