package com.shopsphere.service;

import com.shopsphere.dto.CartDtos.AddCartItemRequest;
import com.shopsphere.dto.CartDtos.CartResponse;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BusinessException;
import com.shopsphere.exception.NotFoundException;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    static final int MAX_QUANTITY_PER_ITEM = 10;

    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;

    public CartService(CartRepository carts, ProductRepository products, UserRepository users) {
        this.carts = carts;
        this.products = products;
        this.users = users;
    }

    @Transactional
    public CartResponse getCart(Long userId) {
        return Mappers.toDto(cartFor(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest req) {
        Cart cart = cartFor(userId);
        Product product = products.findByIdAndActiveTrue(req.productId())
                .orElseThrow(() -> new NotFoundException("Product " + req.productId() + " not found"));
        int newQuantity = cart.findItemByProductId(product.getId()).map(CartItem::getQuantity).orElse(0) + req.quantity();
        validateQuantity(product, newQuantity);
        cart.findItemByProductId(product.getId())
                .ifPresentOrElse(item -> item.setQuantity(newQuantity), () -> cart.addItem(product, req.quantity()));
        return Mappers.toDto(carts.save(cart));
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, int quantity) {
        Cart cart = cartFor(userId);
        CartItem item = cart.findItem(itemId).orElseThrow(() -> new NotFoundException("Cart item " + itemId + " not found"));
        validateQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return Mappers.toDto(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = cartFor(userId);
        CartItem item = cart.findItem(itemId).orElseThrow(() -> new NotFoundException("Cart item " + itemId + " not found"));
        cart.removeItem(item);
        return Mappers.toDto(cart);
    }

    Cart cartFor(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
            return carts.save(new Cart(user));
        });
    }

    private static void validateQuantity(Product product, int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BusinessException("Quantity must be between 1 and " + MAX_QUANTITY_PER_ITEM);
        }
        if (quantity > product.getStockQuantity()) {
            throw new BusinessException("Only " + product.getStockQuantity() + " units of " + product.getName() + " are in stock");
        }
    }
}
