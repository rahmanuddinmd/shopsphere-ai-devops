package com.shopsphere.ai;

import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.service.ProductService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAssistantServiceTest {

    private final Category laptops = new Category("Laptops", "laptops");

    private Product laptop(String name, int price, int ram) {
        Product p = new Product("SKU-" + name.hashCode(), name, laptops, BigDecimal.valueOf(price), 5);
        p.setRamGb(ram);
        return p;
    }

    @Test
    void fallsBackToCatalogAnswerWhenAiDisabled() {
        ProductService productService = mock(ProductService.class);
        when(productService.findAllActive()).thenReturn(List.of());
        when(productService.find(any(), anyInt())).thenReturn(List.of(laptop("Nimbus Pro 14", 68990, 16)));

        var response = new ProductAssistantService(productService, new DisabledAiClient()).ask("laptops under ₹70,000");

        assertThat(response.fallback()).isTrue();
        assertThat(response.provider()).isEqualTo("disabled");
        assertThat(response.answer()).contains("Nimbus Pro 14").contains("₹68,990");
        assertThat(response.products()).hasSize(1);
    }

    @Test
    void namedProductsAreUsedForComparisons() {
        ProductService productService = mock(ProductService.class);
        when(productService.findAllActive()).thenReturn(List.of(
                laptop("Nimbus Pro 14", 68990, 16), laptop("Vertex Book 15", 62490, 16), laptop("Kestrel Air 13", 45990, 8)));
        AiClient ai = new AiClient() {
            public Optional<String> complete(String s, String u) { return Optional.of("Nimbus is better for travel."); }
            public String providerName() { return "stub"; }
            public String modelName() { return "m"; }
        };

        var response = new ProductAssistantService(productService, ai).ask("Compare Nimbus Pro 14 and Vertex Book 15");

        assertThat(response.fallback()).isFalse();
        assertThat(response.products()).extracting("name").containsExactly("Nimbus Pro 14", "Vertex Book 15");
    }

    @Test
    void catalogContextContainsRealPricesOnly() {
        String ctx = ProductAssistantService.toCatalogContext(List.of(laptop("Nimbus Pro 14", 68990, 16)));
        assertThat(ctx).contains("Nimbus Pro 14").contains("₹68,990").contains("RAM: 16 GB");
    }
}
