package deyadecember.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import deyadecember.producer.OrderEventPublisher;
import deyadecember.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Autowired
    private OrderEventPublisher producer;
    @Autowired
    private OrderService service;

    @PostMapping("/create")
    public String createOrder() throws JsonProcessingException {


        service.createOrder(100.0);

        return "Order created!";
    }

}
