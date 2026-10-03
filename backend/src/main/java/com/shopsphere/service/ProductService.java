package com.shopsphere.service;

import com.shopsphere.dto.ProductDtos.CategoryResponse;
import com.shopsphere.dto.ProductDtos.PageResponse;
import com.shopsphere.dto.ProductDtos.ProductRequest;
import com.shopsphere.dto.ProductDtos.ProductResponse;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.BusinessException;
import com.shopsphere.exception.NotFoundException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    /** Search filters shared by the REST API and the AI assistant. */
    public record SearchCriteria(String q, String category, BigDecimal minPrice, BigDecimal maxPrice,
                                 Integer minRam, Boolean inStock) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(SearchCriteria c, int page, int size, String sort) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), toSort(sort));
        Page<Product> result = products.findAll(toSpec(c), pageable);
        return new PageResponse<>(result.map(Mappers::toDto).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** Entity-level search used internally by the AI assistant. */
    @Transactional(readOnly = true)
    public List<Product> find(SearchCriteria c, int limit) {
        return products.findAll(toSpec(c), PageRequest.of(0, limit, Sort.by("price").ascending())).getContent();
    }

    @Transactional(readOnly = true)
    public List<Product> findAllActive() {
        return products.findAll(ProductSpecifications.isActive(), Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return Mappers.toDto(products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found")));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categories.findAll(Sort.by("name")).stream().map(Mappers::toDto).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw new BusinessException("SKU " + req.sku() + " already exists");
        }
        Product p = new Product(req.sku(), req.name(), category(req.categorySlug()), req.price(), req.stockQuantity());
        apply(p, req);
        return Mappers.toDto(products.save(p));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        if (!p.getSku().equals(req.sku()) && products.existsBySku(req.sku())) {
            throw new BusinessException("SKU " + req.sku() + " already exists");
        }
        p.setSku(req.sku());
        p.setName(req.name());
        p.setPrice(req.price());
        p.setStockQuantity(req.stockQuantity());
        p.setCategory(category(req.categorySlug()));
        apply(p, req);
        return Mappers.toDto(p);
    }

    /** Soft delete: past orders keep referencing the product, but it disappears from the shop. */
    @Transactional
    public void delete(Long id) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        p.setActive(false);
    }

    private void apply(Product p, ProductRequest req) {
        p.setBrand(req.brand());
        p.setDescription(req.description());
        p.setMrp(req.mrp());
        p.setImageUrl(req.imageUrl());
        p.setRamGb(req.ramGb());
        p.setStorageGb(req.storageGb());
        p.setProcessor(req.processor());
        p.setDisplay(req.display());
        p.setBadge(req.badge());
        p.setActive(true);
    }

    private Category category(String slug) {
        return categories.findBySlug(slug).orElseThrow(() -> new NotFoundException("Category " + slug + " not found"));
    }

    private static Specification<Product> toSpec(SearchCriteria c) {
        List<Specification<Product>> specs = new ArrayList<>();
        specs.add(ProductSpecifications.isActive());
        specs.add(ProductSpecifications.textContains(c.q()));
        specs.add(ProductSpecifications.inCategory(c.category()));
        specs.add(ProductSpecifications.priceAtLeast(c.minPrice()));
        specs.add(ProductSpecifications.priceAtMost(c.maxPrice()));
        specs.add(ProductSpecifications.ramAtLeast(c.minRam()));
        specs.add(ProductSpecifications.inStockOnly(c.inStock()));
        return Specification.allOf(specs.stream().filter(Objects::nonNull).toList());
    }

    private static Sort toSort(String sort) {
        if (sort == null) {
            return Sort.by("id");
        }
        return switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "newest" -> Sort.by("createdAt").descending();
            case "name" -> Sort.by("name").ascending();
            default -> Sort.by("id");
        };
    }
}
