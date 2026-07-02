package com.foodexpress.order.dto;

import com.foodexpress.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update the status of an order")
public class UpdateStatusRequest {

    @NotNull(message = "status is required")
    @Schema(description = "New order status", example = "CONFIRMED")
    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
