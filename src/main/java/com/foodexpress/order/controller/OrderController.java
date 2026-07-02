package com.foodexpress.order.controller;

import com.foodexpress.order.dto.AssignCourierRequest;
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

        OrderResponse response = orderService.createOrder(user.getUserId(), request);
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

    @PostMapping("/{id}/assign-courier")
    @PreAuthorize("hasAuthority('order.update.status')")
    @Operation(summary = "Assign courier", description = "Assigns a courier to the specified order")
    public ResponseEntity<OrderResponse> assignCourier(
            @PathVariable UUID id,
            @Valid @RequestBody AssignCourierRequest request) {

        OrderResponse response = orderService.assignCourier(id, request.getCourierId());
        return ResponseEntity.ok(response);
    }
}
