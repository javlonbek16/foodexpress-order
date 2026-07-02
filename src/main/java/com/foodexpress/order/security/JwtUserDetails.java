package com.foodexpress.order.security;

import io.jsonwebtoken.Claims;

import java.util.List;
import java.util.UUID;


public class JwtUserDetails {

    private final Long userId;
    private final String role;
    private final UUID restaurantId;
    private final List<String> permissions;
    private final String email;

    public JwtUserDetails(Claims claims, JwtUtil jwtUtil) {
        this.userId = jwtUtil.getUserId(claims);
        this.role = jwtUtil.getRole(claims);
        this.restaurantId = jwtUtil.getRestaurantId(claims);
        this.permissions = jwtUtil.getPermissions(claims);
        this.email = claims.get("email", String.class) != null 
                ? claims.get("email", String.class) 
                : "customer@foodexpress.com";
    }

    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public UUID getRestaurantId() { return restaurantId; }
    public List<String> getPermissions() { return permissions; }
    public String getEmail() { return email; }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equalsIgnoreCase(role);
    }

    public boolean isRestaurant() {
        return "RESTAURANT".equalsIgnoreCase(role);
    }

    public boolean isCourier() {
        return "COURIER".equalsIgnoreCase(role);
    }
}
