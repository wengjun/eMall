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
    private final ConcurrentMap<Long, Long> userIdByOrderId = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        byId.put(order.orderId(), order);
        idByRequest.put(order.requestId(), order.orderId());
        userIdByOrderId.put(order.orderId(), order.userId());
        return order;
    }

    @Override
    public void saveRoute(long orderId, String requestId, long userId) {
        idByRequest.putIfAbsent(requestId, orderId);
        userIdByOrderId.putIfAbsent(orderId, userId);
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
    public Optional<Long> findRouteUserIdByOrderId(long orderId) {
        return Optional.ofNullable(userIdByOrderId.get(orderId));
    }

    @Override
    public Optional<Long> findRouteUserIdByRequestId(String requestId) {
        Long orderId = idByRequest.get(requestId);
        return orderId == null ? Optional.empty() : findRouteUserIdByOrderId(orderId);
    }

    @Override
    public boolean updateStatus(long orderId, OrderStatus expectedStatus, Order order) {
        AtomicFlag updated = new AtomicFlag();
        byId.computeIfPresent(orderId, (id, existing) -> {
            if (existing.status() != expectedStatus) {
                return existing;
            }
            updated.mark();
            return order;
        });
        return updated.value();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int limit) {
        return byId.values().stream().filter(order -> order.status() == status)
                .sorted(Comparator.comparing(Order::updatedAt)).limit(limit).toList();
    }

    private static final class AtomicFlag {
        private boolean value;

        void mark() {
            value = true;
        }

        boolean value() {
            return value;
        }
    }
}
