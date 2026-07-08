package com.foodexpress.order.service;

import com.foodexpress.order.repository.CourierOrderHistoryRepository;
import com.foodexpress.order.repository.OrderRepository;
import com.foodexpress.order.repository.OrderStatusHistoryRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import com.foodexpress.order.exception.ResourceNotFoundException;
import com.foodexpress.order.dto.CourierResponse;
import com.foodexpress.order.dto.AssignCourierRequest;
import com.foodexpress.order.dto.OrderResponse;
import com.foodexpress.order.model.Order;
import com.foodexpress.order.model.CouriersOrderHistory;
import com.foodexpress.order.model.OrderStatus;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourierService {
    private static final Logger log = LoggerFactory.getLogger(CourierService.class);
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CourierOrderHistoryRepository courierOrderHistoryRepository;

    public CourierService(OrderRepository orderRepository, OrderStatusHistoryRepository orderStatusHistoryRepository,
            CourierOrderHistoryRepository courierOrderHistoryRepository) {
        this.orderRepository = orderRepository;
        this.courierOrderHistoryRepository = courierOrderHistoryRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    @Transactional
    public CourierResponse assignToOrder(AssignCourierRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        order.setCourierId(request.getCourierId());
        order.setCourierName(request.getCourierName());
        order.setCourierPhone(request.getPhoneNumber());
        order.setDeliveryStartedAt(Instant.now());
        orderRepository.save(order);

        CouriersOrderHistory history = new CouriersOrderHistory();
        history.setOrderId(request.getOrderId());
        history.setCourierId(request.getCourierId());
        history.setOrderStartedAt(Instant.now());
        history.setDeliveryAddress(order.getDeliveryAddress());
        history.setRestaurantAddress(order.getRestaurantAddress());
        history.setCustomerFullName(order.getCustomerFullName());
        courierOrderHistoryRepository.save(history);

        log.info("Courier {} assigned to order {}", request.getCourierId(), request.getOrderId());

        return toCourierResponse(order);
    }

    public CourierResponse getCurrentOrder(Long courierId) {
        List<Order> orders = orderRepository.findByCourierIdOrderByCreatedAtDesc(courierId);
        Order currentOrder = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.CANCELLED)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active order found for courier: " + courierId));

        return toCourierResponse(currentOrder);
    }

    public List<CourierResponse> getCurrentDeliveringOrders(Long courierId) {
        List<Order> orders = orderRepository.findByCourierIdOrderByCreatedAtDesc(courierId);
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY || order.getStatus() == OrderStatus.DELIVERING)
                .map(this::toCourierResponse)
                .toList();
    }


    public List<CourierResponse> getHistory(Long courierId) {
        List<Order> orders = orderRepository.findByCourierIdOrderByCreatedAtDesc(courierId);
        return orders.stream()
                .map(this::toCourierResponse)
                .toList();
    }

    public List<CourierResponse> getAvailableOrders() {
        List<Order> orders = orderRepository.findByStatusAndCourierIdIsNullOrderByCreatedAtDesc(OrderStatus.READY);
        return orders.stream()
                .map(this::toCourierResponse)
                .toList();
    }

    private CourierResponse toCourierResponse(Order order) {
        CourierResponse response = new CourierResponse();
        response.setOrderId(order.getId());
        if (order.getCourierId() != null) {
            response.setCourierId(order.getCourierId().intValue());
        }
        response.setRestaurantName(order.getRestaurantName());
        response.setCourierName(order.getCourierName());
        response.setPhoneNumber(order.getCourierPhone());
        response.setStatus(order.getStatus());
        response.setDeliveryStartedAt(order.getDeliveryStartedAt());
        response.setDeliveryCompletedAt(order.getDeliveryCompletedAt());
        response.setTotalPrice(order.getTotalPrice());
        response.setCurrency(order.getCurrency());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setRestaurantAddress(order.getRestaurantAddress());
        response.setCustomerFullName(order.getCustomerFullName());
        
        if (response.getDeliveryAddress() == null || response.getRestaurantAddress() == null ||
            response.getCustomerFullName() == null) {
            List<CouriersOrderHistory> histories = courierOrderHistoryRepository.findByOrderIdOrderByOrderStartedAtDesc(order.getId());
            if (!histories.isEmpty()) {
                CouriersOrderHistory history = histories.get(0);
                if (response.getDeliveryAddress() == null) {
                    response.setDeliveryAddress(history.getDeliveryAddress());
                }
                if (response.getRestaurantAddress() == null) {
                    response.setRestaurantAddress(history.getRestaurantAddress());
                }
                if (response.getCustomerFullName() == null) {
                    response.setCustomerFullName(history.getCustomerFullName());
                }
            }
        }

        response.setItems(order.getItems().stream().map(i -> {
            OrderResponse.OrderItemResponse ir = new OrderResponse.OrderItemResponse();
            ir.setId(i.getId());
            ir.setName(i.getName());
            ir.setMenuItemId(i.getMenuItemId());
            ir.setQty(i.getQty());
            ir.setPrice(i.getPrice());
            ir.setImgUrl(i.getImgUrl());
            return ir;
        }).collect(Collectors.toList()));
        return response;
    }

}
