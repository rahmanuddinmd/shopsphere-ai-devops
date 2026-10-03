package com.shopsphere.controller;

import com.shopsphere.dto.CartDtos.AddCartItemRequest;
import com.shopsphere.dto.CartDtos.CartResponse;
import com.shopsphere.dto.CartDtos.UpdateCartItemRequest;
import com.shopsphere.security.CurrentUser;
import com.shopsphere.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(CurrentUser.id(jwt));
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(CurrentUser.id(jwt), request);
    }

    @PutMapping("/items/{itemId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(CurrentUser.id(jwt), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return cartService.removeItem(CurrentUser.id(jwt), itemId);
    }
}
