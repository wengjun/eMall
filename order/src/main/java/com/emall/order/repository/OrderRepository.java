package com.emall.order.repository;

import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(long orderId);

    Optional<Order> findByRequestId(String requestId);

    List<Order> findByStatus(OrderStatus status, int limit);
}
