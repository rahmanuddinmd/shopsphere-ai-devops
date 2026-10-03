package com.shopsphere.repository;

import com.shopsphere.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

/** Composable, type-safe search filters for the product catalog (used by REST search and the AI assistant). */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> textContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("brand")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("processor")), like));
    }

    public static Specification<Product> inCategory(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("slug"), categorySlug);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> ramAtLeast(Integer ramGb) {
        return ramGb == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ramGb"), ramGb);
    }

    public static Specification<Product> inStockOnly(Boolean inStock) {
        return Boolean.TRUE.equals(inStock) ? (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0) : null;
    }
}
