package deyadecember.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import deyadecember.dto.api.CreateOrderRequest;
import deyadecember.dto.api.OrderResponse;
import deyadecember.entities.Order;
import deyadecember.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class OrderController {

    private OrderService service;

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest dto) throws JsonProcessingException {


        Order order =service.createOrder(dto);

        OrderResponse response = new OrderResponse(order.getId(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
