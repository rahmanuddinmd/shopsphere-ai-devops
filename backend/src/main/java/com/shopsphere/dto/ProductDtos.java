package com.shopsphere.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CategoryResponse(Long id, String name, String slug) {
    }

    public record ProductResponse(
            Long id, String sku, String name, String brand, String description,
            BigDecimal price, BigDecimal mrp, int stockQuantity, boolean inStock,
            String imageUrl, Integer ramGb, Integer storageGb, String processor, String display,
            String badge, boolean active, CategoryResponse category) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 40) String sku,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 80) String brand,
            @Size(max = 4000) String description,
            @NotNull @DecimalMin(value = "0.01") BigDecimal price,
            @DecimalMin(value = "0.01") BigDecimal mrp,
            @Min(0) int stockQuantity,
            @Size(max = 500) String imageUrl,
            @Min(0) Integer ramGb,
            @Min(0) Integer storageGb,
            @Size(max = 120) String processor,
            @Size(max = 120) String display,
            @Size(max = 40) String badge,
            @NotBlank String categorySlug) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
