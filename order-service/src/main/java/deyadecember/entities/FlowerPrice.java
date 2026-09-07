package deyadecember.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flower_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowerPrice {

    @Id
    @Column(name = "flower_id", nullable = false, updatable = false)
    private UUID flowerId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Instant updatedAt;
}
