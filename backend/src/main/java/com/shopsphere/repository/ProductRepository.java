package com.shopsphere.repository;

import com.shopsphere.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    boolean existsBySku(String sku);

    List<Product> findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(int threshold);

    long countByActiveTrue();
}
