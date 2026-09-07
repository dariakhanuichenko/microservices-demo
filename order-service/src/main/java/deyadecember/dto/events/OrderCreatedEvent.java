package deyadecember.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private String orderId;
    private String customerId;
    private List<Item> items;
    private BigDecimal totalAmount;
    private Instant createdAt;

    public record Item(String flowerId, int quantity) {}
}

