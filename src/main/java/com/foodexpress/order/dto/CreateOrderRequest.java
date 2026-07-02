package com.foodexpress.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order with items from the frontend")
public class CreateOrderRequest {

    @NotNull(message = "restaurantId is required")
    @Schema(description = "ID of the restaurant")
    private UUID restaurantId;

    @NotNull(message = "Restaurant name is required")
    @Schema(description = "Name of the restaurant", example = "Pizza Palace")
    private String restaurantName;

    @Schema(description = "Currency code (defaults to USD)", example = "USD")
    private String currency = "USD";

    @NotEmpty(message = "items must not be empty")
    @Valid
    @Schema(description = "List of items to order")
    private List<OrderItemRequest> items;

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    @Schema(description = "Individual item in the order request")
    public static class OrderItemRequest {

        @NotNull(message = "menuItemId is required")
        @Schema(description = "ID of the menu item")
        private Long menuItemId;

        @NotNull(message = "name is required")
        @Schema(description = "Name of the item", example = "Cheeseburger")
        private String name;

        @Min(value = 1, message = "qty must be at least 1")
        @Schema(description = "Quantity", example = "2")
        private int qty;

        @NotNull(message = "price is required")
        @Schema(description = "Price per unit", example = "12.50")
        private BigDecimal price;

        public Long getMenuItemId() {
            return menuItemId;
        }

        public void setMenuItemId(Long menuItemId) {
            this.menuItemId = menuItemId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
