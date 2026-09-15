package deyadecember.events;

import deyadecember.entities.CancellationReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CancellationReasonMappingTest {

    @Test
    void allExternalStatusesShouldBeMappable() {
        for (RejectionReason external : RejectionReason.values()) {
            assertDoesNotThrow(() -> CancellationReason.valueOf(external.name()),
                    "There is no value in CancellationReason for : " + external);
        }
    }
}
