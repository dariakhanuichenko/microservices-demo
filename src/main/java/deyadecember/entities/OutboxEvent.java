package deyadecember.entities;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_order_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)

    private UUID id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Column(name = "payload", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private Instant createdAt;
    private boolean processed;
}
