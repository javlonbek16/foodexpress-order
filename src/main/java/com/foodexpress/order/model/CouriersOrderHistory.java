package com.foodexpress.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courier_order_history")
public class CouriersOrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_started_at", nullable = false)
    private Instant orderStartedAt;

    @Column(name = "order_completed_at")
    private Instant orderCompletedAt;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "restaurant_address")
    private String restaurantAddress;

    @Column(name = "customer_full_name")
    private String customerFullName;

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

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setOrderStartedAt(Instant orderStartedAt) {
        this.orderStartedAt = orderStartedAt;
    }

    public void setOrderCompletedAt(Instant orderCompletedAt) {
        this.orderCompletedAt = orderCompletedAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getCourierId() {
        return courierId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getOrderStartedAt() {
        return orderStartedAt;
    }

    public Instant getOrderCompletedAt() {
        return orderCompletedAt;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public void setCustomerFullName(String customerFullName) {
        this.customerFullName = customerFullName;
    }
}
