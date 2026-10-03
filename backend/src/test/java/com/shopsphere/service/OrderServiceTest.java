package com.shopsphere.service;

import com.shopsphere.dto.OrderDtos.CheckoutRequest;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.PaymentMethod;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BusinessException;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orders;
    private CartRepository carts;
    private OrderService service;
    private final CheckoutRequest checkout = new CheckoutRequest("Priya S", "9876543210", "12 MG Road",
            "Hyderabad", "Telangana", "500001", PaymentMethod.CASH_ON_DELIVERY);

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        carts = mock(CartRepository.class);
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new OrderService(orders, carts, new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2026-10-03T10:00:00Z"), ZoneId.of("Asia/Kolkata")));
    }

    private Product product(int price, int stock) {
        return new Product("SKU-1", "Nimbus Pro 14", new Category("Laptops", "laptops"), BigDecimal.valueOf(price), stock);
    }

    @Test
    void placeOrderDecrementsStockTotalsAndClearsCart() {
        Cart cart = new Cart(new User("priya@example.com", "hash", "Priya S"));
        Product p = product(68990, 5);
        cart.addItem(p, 2);
        when(carts.findByUserId(1L)).thenReturn(Optional.of(cart));

        var order = service.placeOrder(1L, checkout);

        assertThat(order.totalAmount()).isEqualByComparingTo("137980");
        assertThat(order.orderNumber()).startsWith("SS-20261003-");
        assertThat(order.items()).hasSize(1);
        assertThat(p.getStockQuantity()).isEqualTo(3);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void rejectsEmptyCart() {
        when(carts.findByUserId(1L)).thenReturn(Optional.of(new Cart(new User("a@b.c", "h", "A"))));
        assertThatThrownBy(() -> service.placeOrder(1L, checkout)).isInstanceOf(BusinessException.class).hasMessageContaining("empty");
    }

    @Test
    void rejectsOverselling() {
        Cart cart = new Cart(new User("priya@example.com", "hash", "Priya S"));
        cart.addItem(product(34999, 1), 3);
        when(carts.findByUserId(1L)).thenReturn(Optional.of(cart));
        assertThatThrownBy(() -> service.placeOrder(1L, checkout)).isInstanceOf(BusinessException.class).hasMessageContaining("in stock");
    }
}
