package deyadecember.entities;

import deyadecember.events.model.RejectionReason;

public enum CancellationReason {
    OUT_OF_STOCK,
    UNKNOWN_FLOWER,
    PAYMENT_FAILED;


    public static CancellationReason fromRejected(RejectionReason external) {
        if (external == null) {
            return null;
        }
        return CancellationReason.valueOf(external.name());
    }
}
