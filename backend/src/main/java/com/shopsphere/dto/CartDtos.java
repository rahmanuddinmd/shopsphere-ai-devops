package com.shopsphere.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(@NotNull Long productId, @Min(1) @Max(10) int quantity) {
    }

    public record UpdateCartItemRequest(@Min(1) @Max(10) int quantity) {
    }

    public record CartItemResponse(Long id, Long productId, String productName, String imageUrl,
                                   BigDecimal unitPrice, int quantity, BigDecimal lineTotal, int stockAvailable) {
    }

    public record CartResponse(Long id, List<CartItemResponse> items, int itemCount, BigDecimal total) {
    }
}
