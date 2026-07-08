package com.foodexpress.order.service;

import com.foodexpress.order.dto.CourierResponse;
import com.foodexpress.order.model.Order;
import com.foodexpress.order.model.OrderStatus;
import com.foodexpress.order.repository.CourierOrderHistoryRepository;
import com.foodexpress.order.repository.OrderRepository;
import com.foodexpress.order.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourierServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CourierOrderHistoryRepository courierOrderHistoryRepository;

    @InjectMocks
    private CourierService courierService;

    @Test
    void getAvailableOrders_success() {
        Order order = new Order();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setStatus(OrderStatus.READY);
        order.setRestaurantName("Pizza Place");

        when(orderRepository.findByStatusAndCourierIdIsNullOrderByCreatedAtDesc(OrderStatus.READY))
                .thenReturn(List.of(order));

        List<CourierResponse> available = courierService.getAvailableOrders();

        assertNotNull(available);
        assertEquals(1, available.size());
        assertEquals(orderId, available.get(0).getOrderId());
        assertEquals("Pizza Place", available.get(0).getRestaurantName());
        assertEquals(OrderStatus.READY, available.get(0).getStatus());
    }
}
