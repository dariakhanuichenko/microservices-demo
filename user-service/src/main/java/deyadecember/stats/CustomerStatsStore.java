package deyadecember.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerStatsStore {

    private static final Duration DEDUP_TTL = Duration.ofDays(7);

    private static final RedisScript<Long> ADD_PAYMENT =
            RedisScript.of(new ClassPathResource("scripts/add-payment.lua"), Long.class);
    private static final RedisScript<Long> REMOVE_PAYMENT =
            RedisScript.of(new ClassPathResource("scripts/remove-payment.lua"), Long.class);

    private final StringRedisTemplate redis;

    public boolean addPayment(UUID paymentId,UUID customerId, BigDecimal amount) {

        Long counted = redis.execute(
                ADD_PAYMENT,
                List.of(processedKey(paymentId), customerKey(customerId)),
                String.valueOf(toCents(amount)),
                String.valueOf(DEDUP_TTL.toSeconds()));
        return Long.valueOf(1).equals(counted);
    }

    public CustomerStats get(UUID customerId) {
        return CustomerStats.fromHash(redis.opsForHash().entries(customerKey(customerId)));
    }

    public RefundResult removePayment(UUID paymentId, UUID customerId, BigDecimal amount) {
        Long result = redis.execute(
                REMOVE_PAYMENT,
                List.of(refundKey(paymentId), customerKey(customerId)),
                String.valueOf(toCents(amount)),
                String.valueOf(DEDUP_TTL.toSeconds()));

        if (Long.valueOf(1).equals(result))  return RefundResult.COUNTED;
        if (Long.valueOf(0).equals(result))  return RefundResult.DUPLICATE;
        return RefundResult.ANOMALY;
    }

    private String refundKey(UUID paymentId) { return "processed:refund:" + paymentId; }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String customerKey(UUID customerId) {
        return "customer:" + customerId;
    }

    private String processedKey(UUID paymentId) {
        return "processed:payment:" + paymentId;
    }
}
