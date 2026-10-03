package com.shopsphere.controller;

import com.shopsphere.dto.OrderDtos.CheckoutRequest;
import com.shopsphere.dto.OrderDtos.OrderResponse;
import com.shopsphere.security.CurrentUser;
import com.shopsphere.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
        return orderService.placeOrder(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.myOrders(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.getOrder(id, CurrentUser.id(jwt), CurrentUser.isAdmin(jwt));
    }
}
