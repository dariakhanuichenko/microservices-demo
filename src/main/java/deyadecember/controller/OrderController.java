package deyadecember.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import deyadecember.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Autowired
    private OrderService service;

    @PostMapping("/create")
    public String createOrder(@RequestParam("amount") String amount) throws JsonProcessingException {


        service.createOrder(Double.parseDouble(amount));

        return "Order created!";
    }

}
