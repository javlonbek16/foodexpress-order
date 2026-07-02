package com.foodexpress.order.exception;

/**
 * Thrown when a requested resource (order, cart item, etc.) is not found.
 * Results in HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
