package com.foodexpress.order.service;

import com.foodexpress.order.config.RabbitMQConfig;
import com.foodexpress.order.model.Order;
import com.foodexpress.order.model.OrderStatus;
import com.foodexpress.order.security.JwtUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private static final String ORDER_CREATED_KEY = "order.created";
    private static final String ORDER_STATUS_CHANGED_KEY = "order.status_changed";

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    private static final String ORDER_CREATED_URL = "https://events.samariddin.uz/v1/events/order-created";
    private static final String ORDER_STATUS_CHANGED_URL = "https://events.samariddin.uz/v1/events/order-status-changed";

    @Value("${gateway.api-key:6Ob_XlkyOKT0YRTJO8L6uXtFOyeC1Y0YtMRfVFNjgtc}")
    private String gatewayApiKey;

    public EventPublisherService(RabbitTemplate rabbitTemplate, RestTemplate restTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
    }


    public void publishOrderCreated(Order order) {

        Map<String, Object> rabbitEvent = new HashMap<>();
        rabbitEvent.put("event", ORDER_CREATED_KEY);
        rabbitEvent.put("timestamp", Instant.now().toString());
        rabbitEvent.put("data", buildOrderData(order));

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, ORDER_CREATED_KEY, rabbitEvent);
            log.info("Published {} event to RabbitMQ for order {}", ORDER_CREATED_KEY, order.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event to RabbitMQ for order {}: {}",
                    ORDER_CREATED_KEY, order.getId(), e.getMessage());
        }


        Map<String, Object> httpEvent = new HashMap<>();
        httpEvent.put("eventId", UUID.randomUUID().toString());
        httpEvent.put("eventType", "order.created");
        httpEvent.put("occurredAt", Instant.now().toString());
        httpEvent.put("version", 0);
        httpEvent.put("data", buildHttpOrderData(order));


        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", gatewayApiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(httpEvent, headers);
            restTemplate.postForEntity(ORDER_CREATED_URL, requestEntity, Void.class);
            log.info("Sent order.created event to external gateway for order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order.created event to external gateway for order {}: {}",
                    order.getId(), e.getMessage());
        }



    }


    public void publishOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {

        Map<String, Object> rabbitEvent = new HashMap<>();
        rabbitEvent.put("event", ORDER_STATUS_CHANGED_KEY);
        rabbitEvent.put("timestamp", Instant.now().toString());

        Map<String, Object> rabbitData = new HashMap<>();
        rabbitData.put("orderId", order.getId().toString());
        rabbitData.put("customerId", order.getCustomerId());
        rabbitData.put("restaurantId", order.getRestaurantId());
        rabbitData.put("restaurantName", order.getRestaurantName());
        rabbitData.put("oldStatus", oldStatus.name());
        rabbitData.put("newStatus", newStatus.name());
        if (order.getCourierId() != null) {
            rabbitData.put("courierId", order.getCourierId());
        }
        rabbitEvent.put("data", rabbitData);

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, ORDER_STATUS_CHANGED_KEY, rabbitEvent);
            log.info("Published {} event to RabbitMQ for order {} ({} → {})",
                    ORDER_STATUS_CHANGED_KEY, order.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to publish {} event to RabbitMQ for order {}: {}",
                    ORDER_STATUS_CHANGED_KEY, order.getId(), e.getMessage());
        }


        Map<String, Object> httpEvent = new HashMap<>();
        httpEvent.put("eventId", UUID.randomUUID().toString());
        httpEvent.put("eventType", "order.status_changed");
        httpEvent.put("occurredAt", Instant.now().toString());
        httpEvent.put("version", 0);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId().toString());
        data.put("customerId", order.getCustomerId());
        data.put("customerEmail", getTargetEmail(order));
        data.put("courierId", order.getCourierId() != null ? order.getCourierId() : 0);
        data.put("oldStatus", oldStatus.name());
        data.put("newStatus", newStatus.name());
        data.put("changedAt", Instant.now().toString());
        data.put("totalPrice", order.getTotalPrice());
        data.put("currency", order.getCurrency() != null ? order.getCurrency() : "UZS");

        httpEvent.put("data", data);


        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", gatewayApiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(httpEvent, headers);
            restTemplate.postForEntity(ORDER_STATUS_CHANGED_URL, requestEntity, Void.class);
            log.info("Sent order.status_changed event to external gateway for order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order.status_changed event to external gateway for order {}: {}",
                    order.getId(), e.getMessage());
        }



    }

    private Map<String, Object> buildHttpOrderData(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId().toString());
        data.put("customerId", order.getCustomerId());
        data.put("customerEmail", getTargetEmail(order));
        data.put("restaurantId", order.getRestaurantId() != null ? order.getRestaurantId().toString() : "");
        data.put("totalPrice", order.getTotalPrice());
        data.put("currency", order.getCurrency() != null ? order.getCurrency() : "UZS");
        data.put("status", order.getStatus().name());
        data.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : Instant.now().toString());

        data.put("items", order.getItems().stream().map(item -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("menuItemId", item.getMenuItemId() != null ? item.getMenuItemId().toString() : "");
            itemMap.put("name", item.getName());
            itemMap.put("qty", item.getQty());
            itemMap.put("price", item.getPrice() != null ? item.getPrice().doubleValue() : 0.0);
            return itemMap;
        }).collect(Collectors.toList()));

        return data;
    }

    private String getTargetEmail(Order order) {
        String email = order.getCustomerEmail();
        if (email == null || email.isBlank()) {
            return "customer@foodexpress.com";
        }
        return email;
    }

    private String getCurrentUserEmail(Long customerId) {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof JwtUserDetails) {
                JwtUserDetails details = (JwtUserDetails) auth.getPrincipal();
                if (details.getEmail() != null) {
                    return details.getEmail();
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve email from security context: {}", e.getMessage());
        }

        return "customer@foodexpress.com";
    }

    private Map<String, Object> buildOrderData(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId().toString());
        data.put("customerId", order.getCustomerId());
        data.put("restaurantId", order.getRestaurantId());
        data.put("restaurantName", order.getRestaurantName());
        data.put("status", order.getStatus().name());
        data.put("totalPrice", order.getTotalPrice());
        data.put("currency", order.getCurrency());
        data.put("createdAt", order.getCreatedAt().toString());

        data.put("items", order.getItems().stream().map(item -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("menuItemId", item.getMenuItemId().toString());
            itemMap.put("name", item.getName());
            itemMap.put("qty", item.getQty());
            itemMap.put("price", item.getPrice());
            return itemMap;
        }).collect(Collectors.toList()));

        return data;
    }
}
