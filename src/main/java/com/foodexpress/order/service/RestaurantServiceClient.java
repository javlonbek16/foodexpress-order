package com.foodexpress.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.foodexpress.order.exception.ResourceNotFoundException;
import com.foodexpress.order.exception.ServiceUnavailableException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;


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


    public BigDecimal validateMenuItem(UUID restaurantId, Long menuItemId) {
        try {
            String url = baseUrl + "/api/menu-items/" + menuItemId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                if (response.containsKey("categories")) {
                    Object categoriesObj = response.get("categories");
                    if (categoriesObj instanceof java.util.List) {
                        java.util.List<?> categories = (java.util.List<?>) categoriesObj;
                        boolean belongsToRestaurant = false;
                        for (Object categoryObj : categories) {
                            if (categoryObj instanceof Map) {
                                Map<?, ?> categoryMap = (Map<?, ?>) categoryObj;
                                Object restaurantUuidObj = categoryMap.get("restaurant");
                                if (restaurantUuidObj != null && restaurantUuidObj.toString().equalsIgnoreCase(restaurantId.toString())) {
                                    belongsToRestaurant = true;
                                    break;
                                }
                            }
                        }
                        if (!belongsToRestaurant) {
                            throw new IllegalArgumentException("Menu item " + menuItemId + " does not belong to restaurant " + restaurantId);
                        }
                    }
                }

                if (response.containsKey("price") && response.get("price") != null) {
                    Object priceObj = response.get("price");
                    return new BigDecimal(priceObj.toString());
                }
            }

            throw new ResourceNotFoundException("Menu item " + menuItemId + " not found");

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Menu item " + menuItemId + " not found");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Menu item " + menuItemId + " not found");
            }
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        }
    }

    public Map<String, Object> getMenuItemData(UUID restaurantId, Long menuItemId) {
        try {
            String url = baseUrl + "/api/menu-items/" + menuItemId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new ResourceNotFoundException("Menu item " + menuItemId + " not found");
            }
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Menu item " + menuItemId + " not found");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Menu item " + menuItemId + " not found");
            }
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        }
    }

    public boolean restaurantExists(UUID restaurantId) {
        try {
            String url = baseUrl + "/api/restaurant/" + restaurantId;
            restTemplate.getForObject(url, Map.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        }
    }

    public String getRestaurantAddress(UUID restaurantId) {
        try {
            String url = baseUrl + "/api/restaurant/" + restaurantId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("address")) {
                Object addressObj = response.get("address");
                if (addressObj != null) {
                    return addressObj.toString();
                }
            }
            throw new IllegalStateException("Restaurant " + restaurantId + " response did not contain an address");
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Restaurant not found: " + restaurantId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Restaurant not found: " + restaurantId);
            }
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Restaurant Service is unavailable: " + e.getMessage());
        }
    }
}
