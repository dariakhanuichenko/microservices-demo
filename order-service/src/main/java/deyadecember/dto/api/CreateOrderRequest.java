package deyadecember.dto.api;

import lombok.*;

import java.util.List;
import java.util.UUID;
public record CreateOrderRequest (
    @NonNull UUID customerId,
    @NonNull List<Item> items) {

    public record Item ( @NonNull UUID flowerId, int quantity) {}

}

