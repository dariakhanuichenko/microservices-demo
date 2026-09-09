package deyadecember.events;

import java.util.UUID;

public record ReservedItem(UUID flowerId, int quantity) {
}
