package com.emall.order.repository;

import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    void saveRoute(long orderId, String requestId, long userId);

    Optional<Order> findById(long orderId);

    Optional<Order> findByRequestId(String requestId);

    Optional<Long> findRouteUserIdByOrderId(long orderId);

    Optional<Long> findRouteUserIdByRequestId(String requestId);

    boolean updateStatus(long orderId, OrderStatus expectedStatus, Order order);

    List<Order> findByStatus(OrderStatus status, int limit);
}
