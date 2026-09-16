package deyadecember.events.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReserved(UUID orderId, List<ReservedItem> items, Instant reservedAt) {
}

