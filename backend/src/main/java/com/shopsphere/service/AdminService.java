package com.shopsphere.service;

import com.shopsphere.ai.AiClient;
import com.shopsphere.dto.AdminDtos.DashboardResponse;
import com.shopsphere.dto.AdminDtos.LowStockItem;
import com.shopsphere.entity.OrderStatus;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.ProductRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AdminService {

    static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository products;
    private final OrderRepository orders;
    private final AiClient aiClient;
    private final ObjectProvider<BuildProperties> buildProperties;

    public AdminService(ProductRepository products, OrderRepository orders, AiClient aiClient,
                        ObjectProvider<BuildProperties> buildProperties) {
        this.products = products;
        this.orders = orders;
        this.aiClient = aiClient;
        this.buildProperties = buildProperties;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Instant since = Instant.now().minus(Duration.ofDays(7));
        var lowStock = products.findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(LOW_STOCK_THRESHOLD).stream()
                .map(p -> new LowStockItem(p.getId(), p.getName(), p.getStockQuantity(), p.getImageUrl()))
                .toList();
        BuildProperties build = buildProperties.getIfAvailable();
        return new DashboardResponse(
                products.countByActiveTrue(),
                orders.countByCreatedAtAfter(since),
                orders.sumRevenueSince(since),
                orders.countByStatus(OrderStatus.PLACED) + orders.countByStatus(OrderStatus.CONFIRMED),
                lowStock,
                aiClient.providerName(),
                build == null ? "dev" : build.getVersion());
    }
}
