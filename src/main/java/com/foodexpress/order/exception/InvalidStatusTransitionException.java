package com.foodexpress.order.exception;

/**
 * Thrown when an invalid order status transition is attempted.
 * Results in HTTP 409 Conflict.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
