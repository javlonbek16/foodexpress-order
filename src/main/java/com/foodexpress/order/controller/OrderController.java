package com.foodexpress.order.controller;


import com.foodexpress.order.dto.CreateOrderRequest;
import com.foodexpress.order.dto.OrderResponse;
import com.foodexpress.order.dto.UpdateStatusRequest;
import com.foodexpress.order.security.JwtUserDetails;
import com.foodexpress.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order.create')")
    @Operation(summary = "Create order", description = "Creates a new order from the customer's cart contents")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal JwtUserDetails user,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(user.getUserId(), user.getEmail(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('order.read.own', 'order.read.all')")
    @Operation(summary = "List orders", description = "Returns orders filtered by the authenticated user's role")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @AuthenticationPrincipal JwtUserDetails user) {

        List<OrderResponse> orders = orderService.getOrders(user);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{userId}")
    @PreAuthorize("hasAnyAuthority('order.read.own', 'order.read.all')")
    @Operation(summary = "Get orders by customer ID", description = "Retrieves all orders for the specified customer, enforcing customer ownership rules")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(
            @PathVariable Long userId,
            @AuthenticationPrincipal JwtUserDetails user) {
        List<OrderResponse> response = orderService.getOrdersByCustomerId(userId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyAuthority('order.read.own', 'order.read.all')")
    @Operation(summary = "Get orders by restaurant ID", description = "Retrieves all orders for the specified restaurant, enforcing restaurant ownership rules")
    public ResponseEntity<List<OrderResponse>> getOrdersByRestaurantId(
            @PathVariable UUID restaurantId,
            @AuthenticationPrincipal JwtUserDetails user) {
        List<OrderResponse> response = orderService.getOrdersByRestaurantId(restaurantId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('order.read.own', 'order.read.all')")
    @Operation(summary = "Get order by ID", description = "Returns a single order, enforcing ownership rules based on role")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserDetails user) {

        OrderResponse response = orderService.getOrderById(id, user);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order.update.status')")
    @Operation(summary = "Update order status", description = "Transitions order to a new status. Invalid transitions return 409 Conflict.")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserDetails user,
            @Valid @RequestBody UpdateStatusRequest request) {

        OrderResponse response = orderService.updateStatus(id, request.getStatus(), user.getUserId().toString());
        return ResponseEntity.ok(response);
    }
}
