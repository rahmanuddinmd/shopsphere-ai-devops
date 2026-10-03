package com.shopsphere.entity;

/** Order lifecycle. PLACED -> CONFIRMED -> SHIPPED -> DELIVERED, or CANCELLED. */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
