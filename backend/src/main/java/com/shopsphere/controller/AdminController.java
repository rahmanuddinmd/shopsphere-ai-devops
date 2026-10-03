package com.shopsphere.controller;

import com.shopsphere.dto.AdminDtos.DashboardResponse;
import com.shopsphere.dto.OrderDtos.OrderResponse;
import com.shopsphere.dto.OrderDtos.UpdateOrderStatusRequest;
import com.shopsphere.dto.ProductDtos.PageResponse;
import com.shopsphere.service.AdminService;
import com.shopsphere.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-only endpoints (enforced in SecurityConfig: /api/admin/** hasRole ADMIN). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;

    public AdminController(AdminService adminService, OrderService orderService) {
        this.adminService = adminService;
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/orders")
    public PageResponse<OrderResponse> orders(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return orderService.allOrders(page, size);
    }

    @PutMapping("/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
