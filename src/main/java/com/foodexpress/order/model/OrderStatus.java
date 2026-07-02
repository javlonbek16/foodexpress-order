package com.foodexpress.order.model;

import java.util.List;

public enum OrderStatus {

    CREATED,
    CONFIRMED,
    PREPARING,
    READY,
    DELIVERING,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        if (next == CANCELLED) {

            return this == CREATED || this == CONFIRMED || this == PREPARING || this == READY;
        }

        return switch (this) {
            case CREATED -> next == CONFIRMED;
            case CONFIRMED -> next == PREPARING;
            case PREPARING -> next == READY;
            case READY -> next == DELIVERING;
            case DELIVERING -> next == DELIVERED;
            default -> false;
        };
    }


    public List<OrderStatus> allowedTransitions() {
        List<OrderStatus> transitions = new java.util.ArrayList<>();

        for (OrderStatus s : values()) {
            if (canTransitionTo(s)) {
                transitions.add(s);
            }
        }
        return transitions;
    }
}
