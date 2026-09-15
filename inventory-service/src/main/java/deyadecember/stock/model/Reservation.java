package deyadecember.stock.model;

import deyadecember.events.RejectionReason;
import deyadecember.events.ReservedItem;

import java.util.List;
import java.util.UUID;

public record Reservation(List<ReservedItem> items,
                          ReservationStatus status,   // life cycle
                          RejectionReason reason,
                          List<UUID> flowerIds) {

    public static Reservation reserved(List<ReservedItem> items) {
        return new Reservation(List.copyOf(items),ReservationStatus.HELD, null, List.of());
    }

    public static Reservation rejected(List<ReservedItem> items,RejectionReason reason, List<UUID> flowerIds) {
        return new Reservation(List.copyOf(items),null, reason, List.copyOf(flowerIds));
    }

    public boolean isReserved() {
        return status != null && reason == null;
    }

    public Reservation withStatus(ReservationStatus next) {
        return new Reservation(items, next, reason, flowerIds);
    }
}