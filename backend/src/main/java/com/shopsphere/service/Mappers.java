package com.shopsphere.service;

import com.shopsphere.dto.AuthDtos.UserResponse;
import com.shopsphere.dto.CartDtos.CartItemResponse;
import com.shopsphere.dto.CartDtos.CartResponse;
import com.shopsphere.dto.OrderDtos.OrderItemResponse;
import com.shopsphere.dto.OrderDtos.OrderResponse;
import com.shopsphere.dto.ProductDtos.CategoryResponse;
import com.shopsphere.dto.ProductDtos.ProductResponse;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;

import java.util.stream.Collectors;

/** Entity -> DTO mapping. Entities never leave the service layer. */
public final class Mappers {

    private Mappers() {
    }

    public static CategoryResponse toDto(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
    }

    public static ProductResponse toDto(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getBrand(), p.getDescription(),
                p.getPrice(), p.getMrp(), p.getStockQuantity(), p.isInStock(), p.getImageUrl(),
                p.getRamGb(), p.getStorageGb(), p.getProcessor(), p.getDisplay(), p.getBadge(), p.isActive(),
                toDto(p.getCategory()));
    }

    public static UserResponse toDto(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toCollection(java.util.TreeSet::new)),
                u.getCreatedAt());
    }

    public static CartResponse toDto(Cart cart) {
        var items = cart.getItems().stream()
                .map(i -> new CartItemResponse(i.getId(), i.getProduct().getId(), i.getProduct().getName(),
                        i.getProduct().getImageUrl(), i.getProduct().getPrice(), i.getQuantity(), i.getLineTotal(),
                        i.getProduct().getStockQuantity()))
                .toList();
        return new CartResponse(cart.getId(), items, cart.getItemCount(), cart.getTotal());
    }

    public static OrderResponse toDto(Order o) {
        var items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct() == null ? null : i.getProduct().getId(),
                        i.getProductName(), i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus(), o.getPaymentMethod(), o.getTotalAmount(),
                o.getCreatedAt(), o.getUser().getFullName(), o.getUser().getEmail(),
                o.getShippingName(), o.getShippingPhone(), o.getShippingAddress(), o.getShippingCity(),
                o.getShippingState(), o.getShippingPin(), items);
    }
}
