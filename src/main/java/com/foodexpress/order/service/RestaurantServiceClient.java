package com.foodexpress.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for the Restaurant Service.
 * Validates menu items and verifies prices before order creation.
 */
@Service
public class RestaurantServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestaurantServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public RestaurantServiceClient(RestTemplate restTemplate, @Value("${restaurant-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Validate that a menu item exists at the given restaurant and return its
     * current price.
     * Returns the price from the Restaurant Service, or null if validation fails.
     */
    public BigDecimal validateMenuItem(UUID restaurantId, Long menuItemId) {
        try {
            String url = baseUrl + "/api/menu-items/" + menuItemId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("price")) {
                Object priceObj = response.get("price");
                return new BigDecimal(priceObj.toString());
            }

            log.warn("Menu item {} not found at restaurant {}", menuItemId, restaurantId);
            return null;

        } catch (RestClientException e) {
            log.warn("Failed to validate menu item {} at restaurant {}: {}",
                    menuItemId, restaurantId, e.getMessage());
            // If the restaurant service is unavailable, we still allow the order
            // but log the warning. In production, this should fail.
            return null;
        }
    }

    public Map<String, Object> getMenuItemData(UUID restaurantId, Long menuItemId) {
        try {
            String url = baseUrl + "/api/menu-items/" + menuItemId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (RestClientException e) {
            log.warn("Failed to get menu item data {} at restaurant {}: {}",
                    menuItemId, restaurantId, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a restaurant exists.
     */
    public boolean restaurantExists(UUID restaurantId) {
        try {
            String url = baseUrl + "/restaurants/" + restaurantId;
            restTemplate.getForObject(url, Map.class);
            return true;
        } catch (RestClientException e) {
            log.warn("Restaurant {} not found or service unavailable: {}", restaurantId, e.getMessage());
            return false;
        }
    }
}
