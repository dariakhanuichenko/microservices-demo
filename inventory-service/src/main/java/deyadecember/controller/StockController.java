package deyadecember.controller;

import deyadecember.stock.StockStore;
import deyadecember.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockStore stockStore;

    @GetMapping
    public Map<UUID, Stock> getCurrentStock() {
        return stockStore.snapshot();
    }

    @GetMapping("/{flowerId}")
    public Stock getCurrentFlowerStore(@PathVariable UUID flowerId) {
        return stockStore.get(flowerId);
    }

    @PostMapping
    public boolean replenishStock() {
        return stockStore.replenishStock();
    }

}
