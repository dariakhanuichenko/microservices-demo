package deyadecember.controller;

import deyadecember.dto.FlowerResponse;
import deyadecember.dto.PriceDTO;
import deyadecember.entities.Flower;
import deyadecember.service.FlowerService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flowers")
@AllArgsConstructor
public class FlowerController {

    private FlowerService service;

    @GetMapping
    public List<Flower> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Flower getFlowerById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<FlowerResponse> updatePrice(@PathVariable UUID id, @RequestBody PriceDTO dto) {

        Flower flower = service.updatePrice(id, dto);
        FlowerResponse response = new FlowerResponse(flower.getId(), flower.getName(),
                flower.getPrice(), flower.getUpdatedAt());
        return ResponseEntity.ok(response);
    }
}
