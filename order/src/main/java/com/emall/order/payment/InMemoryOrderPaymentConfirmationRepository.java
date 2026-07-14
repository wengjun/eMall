package com.emall.order.payment;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryOrderPaymentConfirmationRepository implements OrderPaymentConfirmationRepository {
    private final ConcurrentMap<Long, OrderPaymentConfirmation> confirmations = new ConcurrentHashMap<>();

    @Override
    public OrderPaymentConfirmation saveIfAbsent(OrderPaymentConfirmation confirmation) {
        return confirmations.computeIfAbsent(confirmation.orderId(), ignored -> confirmation);
    }

    @Override
    public Optional<OrderPaymentConfirmation> findByOrderId(long orderId) {
        return Optional.ofNullable(confirmations.get(orderId));
    }
}
