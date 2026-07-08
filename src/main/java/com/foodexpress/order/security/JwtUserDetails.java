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
    private final String phone;
    private final String name;

    public JwtUserDetails(Claims claims, JwtUtil jwtUtil) {
        this.userId = jwtUtil.getUserId(claims);
        this.role = jwtUtil.getRole(claims);
        this.restaurantId = jwtUtil.getRestaurantId(claims);
        this.permissions = jwtUtil.getPermissions(claims);
        String emailClaim = claims.get("email", String.class);
        if (emailClaim == null) {
            String usernameClaim = claims.get("username", String.class);
            if (usernameClaim != null && usernameClaim.contains("@")) {
                emailClaim = usernameClaim;
            } else {
                String subClaim = claims.getSubject();
                if (subClaim != null && subClaim.contains("@")) {
                    emailClaim = subClaim;
                }
            }
        }
        this.email = emailClaim != null ? emailClaim : "customer@foodexpress.com";
        this.phone = claims.get("phone", String.class) != null 
                ? claims.get("phone", String.class) 
                : claims.get("phone_number", String.class);
        this.name = claims.get("name", String.class) != null 
                ? claims.get("name", String.class) 
                : (claims.get("username", String.class) != null 
                        ? claims.get("username", String.class) 
                        : "Courier");
    }

    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public UUID getRestaurantId() { return restaurantId; }
    public List<String> getPermissions() { return permissions; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getName() { return name; }

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
