package com.shopsphere.exception;

/** A request that is well-formed but violates a business rule (e.g. not enough stock). Mapped to HTTP 409. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
