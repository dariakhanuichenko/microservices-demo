package deyadecember.controller;

import deyadecember.dto.FlowerResponse;
import deyadecember.dto.PriceDTO;
import deyadecember.service.FlowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flowers")
@RequiredArgsConstructor
public class FlowerController {

    private final FlowerService service;

    @GetMapping
    public List<FlowerResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FlowerResponse getFlowerById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}/price")
    public FlowerResponse updatePrice(@PathVariable UUID id, @RequestBody PriceDTO dto) {
        return service.updatePrice(id, dto);
    }
}
