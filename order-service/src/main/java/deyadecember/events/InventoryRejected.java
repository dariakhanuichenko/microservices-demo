package deyadecember.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryRejected(
        UUID orderId,
        RejectionReason reason,
        List<UUID> flowerIds,
        Instant rejectedAt) {

}

