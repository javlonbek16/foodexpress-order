package com.foodexpress.order.dto;

import com.foodexpress.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Courier Response")
public class CourierResponse {
    private UUID orderId;
    private Integer courierId;
    private String restaurantName;
    private String courierName;
    private String phoneNumber;
    private String customerFullName;
    private String deliveryAddress;
    private String restaurantAddress;
    private OrderStatus status;
    private Instant deliveryStartedAt;
    private Instant deliveryCompletedAt;
    private List<OrderResponse.OrderItemResponse> items;
    private BigDecimal totalPrice;
    private String currency;

    public void setRestaurantAddress(String restaurantAddress) {
        this.restaurantAddress = restaurantAddress;
    }
    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Integer getCourierId() {
        return courierId;
    }

    public void setCourierId(Integer courierId) {
        this.courierId = courierId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getDeliveryStartedAt() {
        return deliveryStartedAt;
    }

    public void setDeliveryStartedAt(Instant deliveryStartedAt) {
        this.deliveryStartedAt = deliveryStartedAt;
    }

    public Instant getDeliveryCompletedAt() {
        return deliveryCompletedAt;
    }

    public void setDeliveryCompletedAt(Instant deliveryCompletedAt) {
        this.deliveryCompletedAt = deliveryCompletedAt;
    }

    public List<OrderResponse.OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderResponse.OrderItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public void setCustomerFullName(String customerFullName) {
        this.customerFullName = customerFullName;
    }
}
