package deyadecember.controller;

import deyadecember.stats.CustomerStats;
import deyadecember.stats.CustomerStatsStore;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/customers")
@AllArgsConstructor
public class CustomerController {

    private CustomerStatsStore statsStore;

    @GetMapping("/{id}/stats")
    public CustomerStats stats(@PathVariable UUID id) {
        return statsStore.get(id);
    }

}
