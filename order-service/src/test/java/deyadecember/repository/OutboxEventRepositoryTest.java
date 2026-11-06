package deyadecember.repository;


import deyadecember.entities.EventType;
import deyadecember.entities.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OutboxEventRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private OutboxEventRepository repository;

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void returnsOnlyUnprocessedEvents() {
        repository.save(event("a", T0, false));
        repository.save(event("b", T0.plusSeconds(1), true));

        List<OutboxEvent> found = repository.findTop10ByProcessedFalseOrderByCreatedAt();

        assertEquals(1, found.size());
        assertEquals("{\"orderId\":\"a\"}", found.get(0).getPayload(),
                "jsonb payload must round-trip unchanged");
    }

    @Test
    void returnsOldestFirst() {
        repository.save(event("new", T0.plus(2, ChronoUnit.HOURS), false));
        repository.save(event("old", T0, false));

        List<OutboxEvent> found = repository.findTop10ByProcessedFalseOrderByCreatedAt();

        assertEquals("{\"orderId\":\"old\"}", found.get(0).getPayload(),
                "the publisher must drain the outbox in the order events happened");
    }

    @Test
    void returnsAtMostTen() {
        for (int i = 0; i < 12; i++) {
            repository.save(event("e" + i, T0.plusSeconds(i), false));
        }

        assertEquals(10, repository.findTop10ByProcessedFalseOrderByCreatedAt().size());
    }

    private OutboxEvent event(String id, Instant createdAt, boolean processed) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(id)
                .eventType(EventType.ORDER_CREATED)
                .payload("{\"orderId\":\"" + id + "\"}")
                .createdAt(createdAt)
                .processed(processed)
                .build();
    }
}
