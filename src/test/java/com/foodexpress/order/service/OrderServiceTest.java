package com.foodexpress.order.service;

import com.foodexpress.order.dto.CreateOrderRequest;
import com.foodexpress.order.dto.OrderResponse;
import com.foodexpress.order.exception.InvalidStatusTransitionException;
import com.foodexpress.order.exception.ResourceNotFoundException;
import com.foodexpress.order.model.*;
import com.foodexpress.order.repository.OrderRepository;
import com.foodexpress.order.repository.OrderStatusHistoryRepository;
import com.foodexpress.order.security.JwtUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @InjectMocks
    private OrderService orderService;

    private Long customerId;
    private UUID restaurantId;
    private Long menuItemId;

    @BeforeEach
    void setUp() {
        customerId = 1L;
        restaurantId = UUID.randomUUID();
        menuItemId = 3L;
    }

    private CreateOrderRequest buildRequest(BigDecimal price) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(restaurantId);
        request.setCurrency("USD");
        request.setDeliveryAddress("123 Test St");
        request.setCustomerFullName("John Doe");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setMenuItemId(menuItemId);
        item.setName("Burger");
        item.setQty(2);
        item.setPrice(price);
        request.setItems(List.of(item));

        return request;
    }

    @Test
    void createOrder_success_priceUnchanged() {
        CreateOrderRequest request = buildRequest(new BigDecimal("10.00"));

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setCustomerId(customerId);
        savedOrder.setRestaurantId(restaurantId);
        savedOrder.setStatus(OrderStatus.CREATED);
        savedOrder.setCurrency("USD");
        savedOrder.setTotalPrice(new BigDecimal("20.00"));
        savedOrder.setCreatedAt(Instant.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setMenuItemId(menuItemId);
        orderItem.setName("Burger");
        orderItem.setQty(2);
        orderItem.setPrice(new BigDecimal("10.00"));
        savedOrder.addItem(orderItem);

        when(restaurantServiceClient.restaurantExists(restaurantId)).thenReturn(true);
        when(restaurantServiceClient.getRestaurantAddress(restaurantId)).thenReturn("Restaurant Address 123");
        when(restaurantServiceClient.validateMenuItem(restaurantId, menuItemId)).thenReturn(new BigDecimal("10.00"));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(customerId, request);

        assertNotNull(response);
        assertEquals(savedOrder.getId(), response.getId());
        assertEquals(new BigDecimal("20.00"), response.getTotalPrice());
        verify(eventPublisher).publishOrderCreated(savedOrder);
    }

    @Test
    void createOrder_success_priceChanged() {
        CreateOrderRequest request = buildRequest(new BigDecimal("10.00"));

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setCustomerId(customerId);
        savedOrder.setRestaurantId(restaurantId);
        savedOrder.setStatus(OrderStatus.CREATED);
        savedOrder.setCurrency("USD");
        savedOrder.setTotalPrice(new BigDecimal("24.00")); 
        savedOrder.setCreatedAt(Instant.now());

        when(restaurantServiceClient.restaurantExists(restaurantId)).thenReturn(true);
        when(restaurantServiceClient.getRestaurantAddress(restaurantId)).thenReturn("Restaurant Address 123");
        
        when(restaurantServiceClient.validateMenuItem(restaurantId, menuItemId)).thenReturn(new BigDecimal("12.00"));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(customerId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("24.00"), response.getTotalPrice());
        verify(eventPublisher).publishOrderCreated(savedOrder);
    }

    @Test
    void createOrder_emptyItems_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(restaurantId);
        request.setItems(List.of());

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(customerId, request));
    }

    @Test
    void createOrder_nullItems_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(restaurantId);
        request.setItems(null);

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(customerId, request));
    }

    @Test
    void updateStatus_validTransition_success() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(orderId, OrderStatus.CONFIRMED, "admin");

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(eventPublisher).publishOrderStatusChanged(eq(order), eq(OrderStatus.CREATED), eq(OrderStatus.CONFIRMED));
    }

    @Test
    void updateStatus_invalidTransition_throwsException() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStatusTransitionException.class, () -> 
            orderService.updateStatus(orderId, OrderStatus.DELIVERING, "admin")
        );
    }

    @Test
    void getOrderById_unauthorizedCustomer_throwsException() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setCustomerId(999L); 

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        JwtUserDetails user = mock(JwtUserDetails.class);
        when(user.isAdmin()).thenReturn(false);
        when(user.isCustomer()).thenReturn(true);
        when(user.getUserId()).thenReturn(customerId); 

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(orderId, user));
    }

    @Test
    void createOrder_restaurantNotFound_throwsException() {
        CreateOrderRequest request = buildRequest(new BigDecimal("10.00"));

        when(restaurantServiceClient.restaurantExists(restaurantId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(customerId, request));
    }

    @Test
    void createOrder_restaurantServiceUnavailable_throwsException() {
        CreateOrderRequest request = buildRequest(new BigDecimal("10.00"));

        when(restaurantServiceClient.restaurantExists(restaurantId)).thenThrow(
            new com.foodexpress.order.exception.ServiceUnavailableException("Restaurant Service is unavailable")
        );

        assertThrows(com.foodexpress.order.exception.ServiceUnavailableException.class, () -> 
            orderService.createOrder(customerId, request)
        );
    }

    @Test
    void createOrder_menuItemNotFound_throwsException() {
        CreateOrderRequest request = buildRequest(new BigDecimal("10.00"));

        when(restaurantServiceClient.restaurantExists(restaurantId)).thenReturn(true);
        when(restaurantServiceClient.getRestaurantAddress(restaurantId)).thenReturn("Restaurant Address 123");
        when(restaurantServiceClient.validateMenuItem(restaurantId, menuItemId)).thenThrow(
            new ResourceNotFoundException("Menu item 3 not found")
        );

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(customerId, request));
    }
}
