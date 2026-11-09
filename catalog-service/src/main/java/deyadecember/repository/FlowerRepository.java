package deyadecember.repository;

import deyadecember.entities.Flower;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlowerRepository
        extends JpaRepository<Flower, UUID> {

}

