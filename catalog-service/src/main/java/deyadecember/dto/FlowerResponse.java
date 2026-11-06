package deyadecember.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FlowerResponse(
        UUID id,
        String name,
        BigDecimal price,
        Instant updatedAt
) {
}
