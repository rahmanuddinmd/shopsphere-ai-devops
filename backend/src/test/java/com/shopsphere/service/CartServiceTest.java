package com.shopsphere.service;

import com.shopsphere.dto.CartDtos.AddCartItemRequest;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BusinessException;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private final CartRepository carts = mock(CartRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final CartService service = new CartService(carts, products, mock(UserRepository.class));

    private Product product(long id, int stock) {
        Product p = new Product("SKU-" + id, "Aurora X5", new Category("Smartphones", "smartphones"), BigDecimal.valueOf(34999), stock);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void addingSameProductTwiceIncreasesQuantity() {
        Cart cart = new Cart(new User("a@b.c", "h", "A"));
        when(carts.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(carts.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(products.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(product(7L, 5)));

        service.addItem(1L, new AddCartItemRequest(7L, 1));
        var response = service.addItem(1L, new AddCartItemRequest(7L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.itemCount()).isEqualTo(3);
        assertThat(response.total()).isEqualByComparingTo("104997");
    }

    @Test
    void cannotAddMoreThanStock() {
        when(carts.findByUserId(1L)).thenReturn(Optional.of(new Cart(new User("a@b.c", "h", "A"))));
        when(products.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(product(7L, 3)));

        assertThatThrownBy(() -> service.addItem(1L, new AddCartItemRequest(7L, 4)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only 3 units");
    }
}
