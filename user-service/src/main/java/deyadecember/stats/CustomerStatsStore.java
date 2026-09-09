package deyadecember.stats;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomerStatsStore {

    private final Map<UUID, CustomerStats> stats = new ConcurrentHashMap<>();

    public void addPayment(UUID customerId, BigDecimal amount) {
        stats.merge(
                customerId,
                new CustomerStats(1, amount),
                (old, add) -> new CustomerStats(
                        old.ordersCount() + add.ordersCount(),
                        old.totalSpent().add(add.totalSpent())));
    }

    public CustomerStats get(UUID customerId) {
        return stats.getOrDefault(customerId, new CustomerStats(0, BigDecimal.ZERO));
    }
}
