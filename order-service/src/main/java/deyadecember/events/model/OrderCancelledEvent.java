package deyadecember.events.model;

import deyadecember.entities.CancellationReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(UUID orderId, UUID customerId, BigDecimal totalAmount,
                                  CancellationReason reason, Instant cancelledAt) {}
