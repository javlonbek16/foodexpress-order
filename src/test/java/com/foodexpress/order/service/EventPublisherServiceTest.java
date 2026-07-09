package com.foodexpress.order.service;

import com.foodexpress.order.model.Order;
import com.foodexpress.order.model.OrderItem;
import com.foodexpress.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventPublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EventPublisherService eventPublisher;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(123L);
        order.setRestaurantId(UUID.randomUUID());
        order.setRestaurantName("Test Restaurant");
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(new BigDecimal("99.99"));
        order.setCurrency("UZS");
        order.setCreatedAt(Instant.parse("2026-07-09T09:05:17.255Z"));
        order.setCustomerEmail("user@example.com");

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setMenuItemId(3L);
        item.setName("Pizza");
        item.setQty(2);
        item.setPrice(new BigDecimal("49.99"));
        order.addItem(item);
    }

    @Test
    void publishOrderCreated_sendsCorrectHttpPayload() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        eventPublisher.publishOrderCreated(order);

        verify(restTemplate).postForEntity(
                eq("https://events.samariddin.uz/v1/events/order-created"),
                entityCaptor.capture(),
                eq(Void.class)
        );

        HttpEntity<Map<String, Object>> requestEntity = entityCaptor.getValue();
        assertNotNull(requestEntity);
        Map<String, Object> body = requestEntity.getBody();
        assertNotNull(body);

        assertEquals("order.created", body.get("eventType"));
        assertEquals(0, body.get("version"));
        assertNotNull(body.get("eventId"));
        assertNotNull(body.get("occurredAt"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNotNull(data);
        assertEquals(order.getId().toString(), data.get("orderId"));
        assertEquals(order.getCustomerId(), data.get("customerId"));
        assertEquals("user@example.com", data.get("customerEmail"));
        assertEquals(order.getRestaurantId().toString(), data.get("restaurantId"));
        assertEquals(order.getTotalPrice(), data.get("totalPrice"));
        assertEquals("UZS", data.get("currency"));
        assertEquals("CREATED", data.get("status"));
        assertEquals("2026-07-09T09:05:17.255Z", data.get("createdAt"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        assertEquals(1, items.size());
        Map<String, Object> itemMap = items.get(0);
        assertEquals("3", itemMap.get("menuItemId"));
        assertEquals("Pizza", itemMap.get("name"));
        assertEquals(2, itemMap.get("qty"));
        assertEquals(49.99, itemMap.get("price"));
    }

    @Test
    void publishOrderStatusChanged_sendsCorrectHttpPayload() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        order.setStatus(OrderStatus.CONFIRMED);
        eventPublisher.publishOrderStatusChanged(order, OrderStatus.CREATED, OrderStatus.CONFIRMED);

        verify(restTemplate).postForEntity(
                eq("https://events.samariddin.uz/v1/events/order-status-changed"),
                entityCaptor.capture(),
                eq(Void.class)
        );

        HttpEntity<Map<String, Object>> requestEntity = entityCaptor.getValue();
        assertNotNull(requestEntity);
        Map<String, Object> body = requestEntity.getBody();
        assertNotNull(body);

        assertEquals("order.status_changed", body.get("eventType"));
        assertEquals(0, body.get("version"));
        assertNotNull(body.get("eventId"));
        assertNotNull(body.get("occurredAt"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNotNull(data);
        assertEquals(order.getId().toString(), data.get("orderId"));
        assertEquals(order.getCustomerId(), data.get("customerId"));
        assertEquals("user@example.com", data.get("customerEmail"));
        assertEquals(0L, data.get("courierId"));
        assertEquals("CREATED", data.get("oldStatus"));
        assertEquals("CONFIRMED", data.get("newStatus"));
        assertNotNull(data.get("changedAt"));
        assertEquals(order.getTotalPrice(), data.get("totalPrice"));
        assertEquals("UZS", data.get("currency"));
    }
}
