package deyadecember.repository;

import deyadecember.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       UPDATE Payment p
          SET p.status = deyadecember.entities.PaymentStatus.REFUNDED,
              p.refundedAt = :now
        WHERE p.orderId = :orderId
          AND p.status = deyadecember.entities.PaymentStatus.COMPLETED
       """)
    int markRefunded(@Param("orderId") UUID orderId, @Param("now")Instant now);
}

