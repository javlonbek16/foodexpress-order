package com.foodexpress.order.controller;

import com.foodexpress.order.service.CourierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.foodexpress.order.dto.CourierResponse;
import com.foodexpress.order.dto.AssignCourierRequest;
import com.foodexpress.order.security.JwtUserDetails;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/courier")
@Tag(name = "Courier", description = "Courier API")
@SecurityRequirement(name = "bearerAuth")
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @PostMapping("/assign-to-order")
    @PreAuthorize("hasAuthority('order.update.status')")
    @Operation(summary = "Assign to order", description = "Assigns the authenticated courier to the specified order")
    public ResponseEntity<CourierResponse> assignToOrder(
            @AuthenticationPrincipal JwtUserDetails user,
            @Valid @RequestBody AssignCourierRequest request) {
        
        
        request.setCourierId(user.getUserId());
        request.setPhoneNumber(user.getPhone());
        request.setCourierName(user.getName());

        CourierResponse response = courierService.assignToOrder(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/delivering-orders")
    @PreAuthorize("hasAuthority('order.update.status')")
    @Operation(summary = "Get delivering orders", description = "Retrieves the all delivering orders assigned to the authenticated courier")
    public ResponseEntity<List<CourierResponse>> getCurrentDeliveringOrders(
            @AuthenticationPrincipal JwtUserDetails user) {
        List<CourierResponse> response = courierService.getCurrentDeliveringOrders(user.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available-orders")
    @PreAuthorize("hasAuthority('order.update.status')")
    @Operation(summary = "Get available orders", description = "Retrieves a list of READY orders that have no courier assigned")
    public ResponseEntity<List<CourierResponse>> getAvailableOrders() {
        List<CourierResponse> response = courierService.getAvailableOrders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('order.update.status')")
    @Operation(summary = "Get assignment history", description = "Retrieves the assignment history of the authenticated courier")
    public ResponseEntity<List<CourierResponse>> getHistory(
            @AuthenticationPrincipal JwtUserDetails user) {
        List<CourierResponse> response = courierService.getHistory(user.getUserId());
        return ResponseEntity.ok(response);
    }
}
