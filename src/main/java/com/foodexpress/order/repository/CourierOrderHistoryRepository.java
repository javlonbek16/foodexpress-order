package com.foodexpress.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.foodexpress.order.model.CouriersOrderHistory;

@Repository
public interface CourierOrderHistoryRepository extends JpaRepository<CouriersOrderHistory, UUID> {
    List<CouriersOrderHistory> findByOrderIdOrderByOrderStartedAtDesc(UUID orderId);
}
