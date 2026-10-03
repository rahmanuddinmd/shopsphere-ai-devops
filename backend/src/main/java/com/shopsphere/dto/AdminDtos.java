package com.shopsphere.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record LowStockItem(Long id, String name, int stockQuantity, String imageUrl) {
    }

    public record DashboardResponse(long activeProducts, long ordersLast7Days, BigDecimal revenueLast7Days,
                                    long ordersAwaitingShipment, List<LowStockItem> lowStock,
                                    String aiProvider, String version) {
    }
}
