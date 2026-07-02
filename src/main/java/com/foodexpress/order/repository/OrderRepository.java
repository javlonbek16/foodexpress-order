package com.foodexpress.order.repository;

import com.foodexpress.order.model.Order;
import com.foodexpress.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    List<Order> findByCourierIdOrderByCreatedAtDesc(Long courierId);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
