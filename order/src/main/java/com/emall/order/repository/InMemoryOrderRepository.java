package com.emall.order.repository;

import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentMap<Long, Order> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> idByRequest = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        byId.put(order.orderId(), order);
        idByRequest.put(order.requestId(), order.orderId());
        return order;
    }

    @Override
    public Optional<Order> findById(long orderId) {
        return Optional.ofNullable(byId.get(orderId));
    }

    @Override
    public Optional<Order> findByRequestId(String requestId) {
        Long orderId = idByRequest.get(requestId);
        return orderId == null ? Optional.empty() : findById(orderId);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int limit) {
        return byId.values().stream()
                .filter(order -> order.status() == status)
                .sorted(Comparator.comparing(Order::updatedAt))
                .limit(limit)
                .toList();
    }
}
