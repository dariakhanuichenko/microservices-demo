package deyadecember.events.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private String orderId;
    private String customerId;
    private List<Item> items;
    private BigDecimal totalAmount;
    private Instant createdAt;

    public record Item(UUID flowerId, int quantity) {}
}

