package deyadecember.stock;

import deyadecember.events.RejectionReason;
import deyadecember.events.ReservedItem;
import deyadecember.stock.model.Reservation;
import deyadecember.stock.model.ReservationStatus;
import deyadecember.stock.model.Stock;
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
    private final Map<UUID, Reservation> byOrder = new ConcurrentHashMap<>(); // which order made reservation, key=orderId

    @PostConstruct
    void seed() {
        replenishStock();
    }

    public synchronized Reservation reserve(UUID orderId, List<ReservedItem> items) {
        Reservation existing = byOrder.get(orderId);
        if (existing != null) {
            log.info("Order {} already reserved, replaying status {}", orderId, existing.status());
            return existing;
        }

        List<UUID> unknown = items.stream()
                .map(ReservedItem::flowerId)
                .filter(id -> !stock.containsKey(id))
                .toList();
        if (!unknown.isEmpty()) {
            log.error("Unknown flowers in order {}: {}", orderId, unknown);
            return remember(orderId,
                    Reservation.rejected(items, RejectionReason.UNKNOWN_FLOWER, unknown));
        }

        List<UUID> insufficient = items.stream()
                .filter(i -> stock.get(i.flowerId()).available() < i.quantity())
                .map(ReservedItem::flowerId)
                .toList();
        if (!insufficient.isEmpty()) {
            log.warn("Not enough stock for order {}: {}", orderId, insufficient);
            return remember(orderId,
                    Reservation.rejected(items, RejectionReason.OUT_OF_STOCK, insufficient));
        }

        //reserve in stock
        items.forEach(reserve ->
                stock.computeIfPresent(reserve.flowerId(), (k, current) ->
                        new Stock(current.available() - reserve.quantity(),
                                current.reserved() + reserve.quantity())));

        // add to byOrder
        return remember(orderId, Reservation.reserved(items));
    }

    private Reservation remember(UUID orderId, Reservation reservation) {
        byOrder.put(orderId, reservation);
        return reservation;
    }

    public Stock get(UUID flowerId) {
        return stock.get(flowerId);
    }

    public boolean replenishStock() {
        log.warn("creating new stock, cleaning order by");
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
        byOrder.clear();
        return true;
    }

    public synchronized void setStock(UUID flowerId, Stock value) {
        stock.put(flowerId, value);
    }

    public Map<UUID, Stock> snapshot() {
        return Map.copyOf(stock);
    }

    //no payment --> rollback inventory state
    public synchronized void release(UUID orderId) {
        log.info("Rollback inventory state for order {}", orderId);
        Reservation reservation = byOrder.get(orderId);

        if (reservation == null) {
            throw new IllegalStateException("No reservation yet for order " + orderId);
        }
        if (reservation.status() != ReservationStatus.HELD) {
            log.info("Order {} is {}, release ignored", orderId, reservation.status());
            return;
        }

        List<ReservedItem> items = reservation.items();
        items.forEach(reserve ->
                stock.computeIfPresent(reserve.flowerId(), (k, current) ->
                        new Stock(current.available() + reserve.quantity(),
                                current.reserved() - reserve.quantity())));

        byOrder.put(orderId, reservation.withStatus(ReservationStatus.RELEASED));
    }

    public synchronized void confirm(UUID orderId) {

        Reservation reservation = byOrder.get(orderId);

        if (reservation == null) {
            throw new IllegalStateException("No reservation yet for order " + orderId);
        }

        if (!reservation.isReserved()) {
            log.warn("Order {} was never reserved ({}), cannot confirm", orderId, reservation.reason());
            return;
        }

        if (reservation.status() != ReservationStatus.HELD) {
            log.info("Order {} is {}, confirm ignored", orderId, reservation.status());
            return;
        }
        List<ReservedItem> items = reservation.items();
        items.forEach(reserve ->
                stock.computeIfPresent(reserve.flowerId(), (k, current) ->
                        new Stock(current.available(),
                                current.reserved() - reserve.quantity()))); // reserved flowers are purchased

        byOrder.put(orderId, reservation.withStatus(ReservationStatus.CONFIRMED));
        log.info("Order {} confirmed, {} positions shipped", orderId, reservation.items().size());
    }

}