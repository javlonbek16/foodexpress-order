package com.foodexpress.order.service;

import com.foodexpress.order.dto.CreateOrderRequest;
import com.foodexpress.order.dto.OrderResponse;
import com.foodexpress.order.exception.InvalidStatusTransitionException;
import com.foodexpress.order.exception.ResourceNotFoundException;
import com.foodexpress.order.model.*;
import com.foodexpress.order.repository.OrderRepository;
import com.foodexpress.order.repository.OrderStatusHistoryRepository;
import com.foodexpress.order.security.JwtUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final EventPublisherService eventPublisher;
    private final RestaurantServiceClient restaurantServiceClient;

    public OrderService(OrderRepository orderRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            EventPublisherService eventPublisher,
            RestaurantServiceClient restaurantServiceClient) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.restaurantServiceClient = restaurantServiceClient;
    }

    @Transactional
    public OrderResponse createOrder(Long customerId, CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot create an order with no items");
        }

        if (!restaurantServiceClient.restaurantExists(request.getRestaurantId())) {
            log.warn("Restaurant {} not found or Restaurant Service is unavailable. Proceeding with order creation.",
                    request.getRestaurantId());
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setRestaurantId(request.getRestaurantId());
        order.setRestaurantName(request.getRestaurantName());
        order.setStatus(OrderStatus.CREATED);
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            BigDecimal currentPrice = restaurantServiceClient.validateMenuItem(request.getRestaurantId(),
                    itemRequest.getMenuItemId());

            BigDecimal finalPrice = itemRequest.getPrice();
            if (currentPrice != null) {

                if (currentPrice.compareTo(itemRequest.getPrice()) != 0) {
                    log.info("Price updated for item {} from {} to {}", itemRequest.getMenuItemId(),
                            itemRequest.getPrice(), currentPrice);
                    finalPrice = currentPrice;
                }
            } else {
                log.warn("Could not validate menu item {} at restaurant {}. Using provided price as fallback.",
                        itemRequest.getMenuItemId(), request.getRestaurantId());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(itemRequest.getMenuItemId());
            orderItem.setName(itemRequest.getName());
            orderItem.setQty(itemRequest.getQty());
            orderItem.setPrice(finalPrice);
            order.addItem(orderItem);

            totalPrice = totalPrice.add(finalPrice.multiply(BigDecimal.valueOf(itemRequest.getQty())));
        }
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        statusHistoryRepository.save(
                new OrderStatusHistory(savedOrder.getId(), null, OrderStatus.CREATED, customerId.toString()));

        eventPublisher.publishOrderCreated(savedOrder);

        log.info("Order {} created by customer {} for restaurant {}",
                savedOrder.getId(), customerId, request.getRestaurantId());

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(JwtUserDetails user) {
        List<Order> orders;

        if (user.isAdmin()) {
            orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if (user.isCustomer()) {
            orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getUserId());
        } else if (user.isRestaurant()) {
            orders = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(user.getRestaurantId());
        } else if (user.isCourier()) {
            orders = orderRepository.findByCourierIdOrderByCreatedAtDesc(user.getUserId());
        } else {
            orders = List.of();
        }

        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, JwtUserDetails user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!user.isAdmin()) {
            if (user.isCustomer() && !order.getCustomerId().equals(user.getUserId())) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }
            if (user.isRestaurant() && !order.getRestaurantId().equals(user.getRestaurantId())) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }
            if (user.isCourier() && !user.getUserId().equals(order.getCourierId())) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition: " + oldStatus + " → " + newStatus);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        statusHistoryRepository.save(new OrderStatusHistory(orderId, oldStatus, newStatus, changedBy));

        eventPublisher.publishOrderStatusChanged(order, oldStatus, newStatus);

        log.info("Order {} status changed: {} → {}", orderId, oldStatus, newStatus);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse assignCourier(UUID orderId, Long courierId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        order.setCourierId(courierId);
        orderRepository.save(order);

        log.info("Courier {} assigned to order {}", courierId, orderId);

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setRestaurantId(order.getRestaurantId());
        response.setRestaurantName(order.getRestaurantName());
        response.setCourierId(order.getCourierId());
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        response.setCurrency(order.getCurrency());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setItems(order.getItems().stream().map(i -> {
            OrderResponse.OrderItemResponse ir = new OrderResponse.OrderItemResponse();
            ir.setId(i.getId());
            ir.setName(i.getName());
            ir.setMenuItemId(i.getMenuItemId());
            ir.setQty(i.getQty());
            ir.setPrice(i.getPrice());
            return ir;
        }).collect(Collectors.toList()));
        return response;
    }
}
