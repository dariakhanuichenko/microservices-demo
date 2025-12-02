package deyadecember.controller;

import deyadecember.dto.OrderCreatedEvent;
import deyadecember.producer.OrderProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class OrderController {

    @Autowired
    private OrderProducer producer;

    @PostMapping("/create")
    public String createOrder() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                100.0
        );

        producer.send(event);

        return "Order created!";
    }

}
