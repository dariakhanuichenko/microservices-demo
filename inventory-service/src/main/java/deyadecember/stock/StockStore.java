package deyadecember.stock;

import deyadecember.events.ReservedItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockStore {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(StockStore.class);

    private final Map<UUID, Stock> stock = new ConcurrentHashMap<>();   // available and reserved flower, key =flowerId
    private final Map<UUID, List<ReservedItem>> byOrder = new ConcurrentHashMap<>(); // which order made reservation, key=orderId

    @PostConstruct
    void seed() {
        replenishStock();
    }

    public synchronized boolean reserve(UUID orderId, List<ReservedItem> items) {
        if (byOrder.containsKey(orderId)) {
            log.warn("Order {} already reserved, skipping", orderId);
            return false;
        }

        List<UUID> unknown = items.stream()
                .map(ReservedItem::flowerId)
                .filter(id -> !stock.containsKey(id))
                .toList();
        if (!unknown.isEmpty()) {
            log.error("Unknown flowers in order {}: {}", orderId, unknown);
            return false;
        }

        List<UUID> insufficient = items.stream()
                .filter(i -> stock.get(i.flowerId()).available() < i.quantity())
                .map(ReservedItem::flowerId)
                .toList();
        if (!insufficient.isEmpty()) {
            log.warn("Not enough stock for order {}: {}", orderId, insufficient);
            return false;
        }

        // add to byOrder
        byOrder.put(orderId, List.copyOf(items));

        //reserve in stock
        items.forEach(reserve ->
            stock.computeIfPresent(reserve.flowerId(), (k, current) ->
                    new Stock(current.available() - reserve.quantity(),
                            current.reserved() + reserve.quantity())));
        return true;

    }

    public Stock get(UUID flowerId) {
        return stock.get(flowerId);
    }

    public boolean replenishStock() {
        //11111111-1111-1111-1111-111111111111  Троянда
        //22222222-2222-2222-2222-222222222222  Тюльпан
        //33333333-3333-3333-3333-333333333333  Півонія
        //44444444-4444-4444-4444-444444444444  Гортензія
        //55555555-5555-5555-5555-555555555555  Ромашка
        stock.put(UUID.fromString("11111111-1111-1111-1111-111111111111"), new Stock(20, 0));
        stock.put(UUID.fromString("22222222-2222-2222-2222-222222222222"), new Stock(30, 0));
        stock.put(UUID.fromString("33333333-3333-3333-3333-333333333333"), new Stock(40, 0));
        stock.put(UUID.fromString("44444444-4444-4444-4444-444444444444"), new Stock(20, 0));
        stock.put(UUID.fromString("55555555-5555-5555-5555-555555555555"), new Stock(15, 0));
        return true;
    }

    public synchronized void setStock(UUID flowerId, Stock value) {
        stock.put(flowerId, value);
    }

    public Map<UUID, Stock> snapshot() {
        return Map.copyOf(stock);
    }

    public synchronized void release(UUID orderId) { //todo implement
    }

    public synchronized void confirm(UUID orderId) { //todo implement
    }
}