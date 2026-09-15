package deyadecember.stats;

import java.math.BigDecimal;
import java.util.Map;

public record CustomerStats (int ordersCount, BigDecimal totalSpent) {
    public static CustomerStats empty() {
        return new CustomerStats(0, BigDecimal.ZERO);
    }

    public static CustomerStats fromHash(Map<Object, Object> hash) {
        if (hash.isEmpty()) {
            return empty();
        }
        int count = Integer.parseInt((String) hash.getOrDefault("ordersCount", "0"));
        long cents = Long.parseLong((String) hash.getOrDefault("totalSpentCents", "0"));
        return new CustomerStats(count, BigDecimal.valueOf(cents, 2));
    }
}
