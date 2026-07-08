package com.foodexpress.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to assign a courier to an order")
public class AssignCourierRequest {

    @NotNull(message = "courierId is required")
    @Schema(description = "ID of the courier to assign")
    private Long courierId;

    @NotNull(message = "orderId is required")
    @Schema(description = "ID of the order to assign")
    private UUID orderId;

    @Schema(description = "Name of the courier")
    private String courierName;

    @Schema(description = "Phone number of the courier")
    private String phoneNumber;

    public Long getCourierId() {
        return courierId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}
