package deyadecember.stats;

import java.math.BigDecimal;

public record CustomerStats (int ordersCount, BigDecimal totalSpent) {
}
