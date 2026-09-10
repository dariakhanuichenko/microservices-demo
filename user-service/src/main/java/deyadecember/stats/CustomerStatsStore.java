package deyadecember.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CustomerStatsStore {

    private final StringRedisTemplate redis;
    private final Map<UUID, CustomerStats> stats = new ConcurrentHashMap<>();

    public void addPayment(UUID customerId, BigDecimal amount) {
        long cents = amount.movePointRight(2).longValueExact();
        redis.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations ops) {

                ops.multi(); //atomicity
                ops.opsForHash().increment(key(customerId), "ordersCount", 1);
                ops.opsForHash().increment(key(customerId), "totalSpentCents", cents);
                return ops.exec();
            }
        });
    }

    public CustomerStats get(UUID customerId) {
        Map<Object, Object> h = redis.opsForHash().entries(key(customerId));
        if (h.isEmpty()) {
            return new CustomerStats(0, BigDecimal.ZERO);
        }
        int count = Integer.parseInt((String) h.getOrDefault("ordersCount", "0"));
        long cents = Long.parseLong((String) h.getOrDefault("totalSpentCents", "0"));
        return new CustomerStats(count, BigDecimal.valueOf(cents, 2));
    }

    private String key(UUID customerId) {
        return "customer:" + customerId;
    }
}
