package deyadecember.stock;

import deyadecember.stock.model.Stock;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

abstract class StockStoreTestBase {

    protected static final UUID ROSE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    protected static final UUID TULIP = UUID.fromString("22222222-2222-2222-2222-222222222222");
    protected static final UUID UNKNOWN_FLOWER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    protected static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    protected StockStore stockStore;

    @BeforeEach
    void setUp() {
        stockStore = new StockStore();
        stockStore.setStock(ROSE, new Stock(2, 0));
        stockStore.setStock(TULIP, new Stock(3, 0));
    }

    public int getGeneralSum(Stock stock) {
        return stock.available() + stock.reserved();
    }
}
