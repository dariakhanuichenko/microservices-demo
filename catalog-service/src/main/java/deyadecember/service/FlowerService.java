package deyadecember.service;


import deyadecember.dto.PriceDTO;
import deyadecember.entities.Flower;
import deyadecember.repository.FlowerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlowerService {

    private final FlowerRepository flowerRepository;
    private final Logger log = org.slf4j.LoggerFactory.getLogger(FlowerService.class);


    public List<Flower> getAll() {
        return new ArrayList<>(flowerRepository.findAll());
    }

    public Flower getById(UUID id) {
        Flower flower = flowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Flower not found: " + id
                ));

        return new Flower(flower);
    }

    @Transactional
    public Flower updatePrice(UUID id, PriceDTO dto) {
        if (dto == null || dto.price() == null || dto.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid price found: " + dto);
        }
        Flower flower = flowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Flower not found: " + id));
        flower.setPrice(dto.price());
        flower.setUpdatedAt(Instant.now());
        return flower;
    }

}
