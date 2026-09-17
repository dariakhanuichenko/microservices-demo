package deyadecember.controller;

import deyadecember.stats.CustomerStats;
import deyadecember.stats.CustomerStatsStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor

public class CustomerController {

    private final CustomerStatsStore statsStore;

    @GetMapping("/{id}/stats")
    public CustomerStats stats(@PathVariable UUID id) {
        return statsStore.get(id);
    }

}
