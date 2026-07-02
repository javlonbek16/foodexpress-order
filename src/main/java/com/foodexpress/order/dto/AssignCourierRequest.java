package com.foodexpress.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to assign a courier to an order")
public class AssignCourierRequest {

    @NotNull(message = "courierId is required")
    @Schema(description = "ID of the courier to assign")
    private Long courierId;

    public Long getCourierId() {
        return courierId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }
}
