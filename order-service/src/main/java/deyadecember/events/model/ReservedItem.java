package deyadecember.events.model;

import java.util.UUID;

public record ReservedItem(UUID flowerId, int quantity) {
}
