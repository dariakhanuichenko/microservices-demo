package deyadecember.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record PaymentFailed(UUID paymentId, UUID orderId, UUID customerId, BigDecimal amount, Instant failedAt) {
}

