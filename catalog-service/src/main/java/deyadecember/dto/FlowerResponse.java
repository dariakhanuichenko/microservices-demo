package deyadecember.dto;

import deyadecember.entities.Flower;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FlowerResponse(
        UUID id,
        String name,
        BigDecimal price,
        Instant updatedAt
) {
    public static FlowerResponse from(Flower flower) {
        return new FlowerResponse(flower.getId(), flower.getName(),
                flower.getPrice(), flower.getUpdatedAt());
    }
}
