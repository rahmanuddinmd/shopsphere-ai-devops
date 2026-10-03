package com.shopsphere.service;

import com.shopsphere.dto.OrderDtos.CheckoutRequest;
import com.shopsphere.dto.OrderDtos.OrderResponse;
import com.shopsphere.dto.ProductDtos.PageResponse;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.OrderStatus;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.BusinessException;
import com.shopsphere.exception.NotFoundException;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository carts;
    private final Counter ordersPlaced;
    private final Clock clock;

    public OrderService(OrderRepository orders, CartRepository carts, MeterRegistry meterRegistry) {
        this(orders, carts, meterRegistry, Clock.system(ZoneId.of("Asia/Kolkata")));
    }

    OrderService(OrderRepository orders, CartRepository carts, MeterRegistry meterRegistry, Clock clock) {
        this.orders = orders;
        this.carts = carts;
        this.clock = clock;
        // Business metric visible in Prometheus as shopsphere_orders_placed_total
        this.ordersPlaced = Counter.builder("shopsphere.orders.placed").description("Orders placed").register(meterRegistry);
    }

    /**
     * Checkout in one transaction: validate stock, decrement stock, create the order, empty the cart.
     * Product @Version ensures two simultaneous checkouts cannot oversell the last unit.
     */
    @Transactional
    public OrderResponse placeOrder(Long userId, CheckoutRequest req) {
        Cart cart = carts.findByUserId(userId).orElseThrow(() -> new BusinessException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty");
        }
        Order order = new Order(nextOrderNumber(), cart.getUser(), req.paymentMethod());
        order.setShipping(req.fullName().trim(), req.phone().trim(), req.address().trim(),
                req.city().trim(), req.state().trim(), req.pin().trim());
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new BusinessException(product.getName() + " is no longer available");
            }
            if (item.getQuantity() > product.getStockQuantity()) {
                throw new BusinessException("Only " + product.getStockQuantity() + " units of " + product.getName() + " are in stock");
            }
            product.decreaseStock(item.getQuantity());
            order.addItem(product, item.getQuantity());
        }
        Order saved = orders.save(order);
        cart.clear();
        ordersPlaced.increment();
        return Mappers.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(Mappers::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long userId, boolean admin) {
        Order order = (admin ? orders.findById(orderId) : orders.findByIdAndUserId(orderId, userId))
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        return Mappers.toDto(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> allOrders(int page, int size) {
        Page<Order> result = orders.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new PageResponse<>(result.map(Mappers::toDto).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order " + order.getOrderNumber() + " is already " + order.getStatus());
        }
        if (status == OrderStatus.CANCELLED) {
            order.getItems().forEach(i -> {
                if (i.getProduct() != null) {
                    i.getProduct().setStockQuantity(i.getProduct().getStockQuantity() + i.getQuantity());
                }
            });
        }
        order.setStatus(status);
        return Mappers.toDto(order);
    }

    private String nextOrderNumber() {
        String date = LocalDate.now(clock).toString().replace("-", "");
        return "SS-" + date + "-" + (1000 + ThreadLocalRandom.current().nextInt(9000));
    }
}
