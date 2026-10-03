package com.shopsphere.dto;

import com.shopsphere.entity.OrderStatus;
import com.shopsphere.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Pattern(regexp = "^[0-9+ -]{7,20}$", message = "must be a valid phone number") String phone,
            @NotBlank @Size(max = 300) String address,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be a 6-digit PIN code") String pin,
            @NotNull PaymentMethod paymentMethod) {
    }

    public record OrderItemResponse(Long productId, String productName, String imageUrl,
                                    BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record OrderResponse(Long id, String orderNumber, OrderStatus status, PaymentMethod paymentMethod,
                                BigDecimal totalAmount, Instant createdAt, String customerName, String customerEmail,
                                String shippingName, String shippingPhone, String shippingAddress,
                                String shippingCity, String shippingState, String shippingPin,
                                List<OrderItemResponse> items) {
    }

    public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
    }
}
