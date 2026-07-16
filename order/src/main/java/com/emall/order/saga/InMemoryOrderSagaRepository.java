package com.emall.order.saga;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryOrderSagaRepository implements OrderSagaRepository {
    private final ConcurrentMap<String, OrderCreateSaga> sagas = new ConcurrentHashMap<>();

    @Override
    public OrderCreateSaga save(OrderCreateSaga saga) {
        if (saga.version() == 0L) {
            return sagas.computeIfAbsent(saga.requestId(), ignored -> saga);
        }
        sagas.compute(saga.requestId(), (requestId, current) -> {
            if (current == null || current.sagaId() != saga.sagaId() || current.version() != saga.version() - 1) {
                throw new OrderSagaConcurrencyException(saga.requestId(), saga.version() - 1);
            }
            return saga;
        });
        return saga;
    }

    @Override
    public Optional<OrderCreateSaga> findByRequestId(String requestId) {
        return Optional.ofNullable(sagas.get(requestId));
    }

    @Override
    public List<OrderCreateSaga> findRecoverable(Instant staleBefore, Instant retryBefore, int limit) {
        return sagas.values().stream().filter(saga -> recoverable(saga, staleBefore, retryBefore))
                .sorted(Comparator.comparing(OrderCreateSaga::updatedAt)).limit(limit).toList();
    }

    private boolean recoverable(OrderCreateSaga saga, Instant staleBefore, Instant retryBefore) {
        if (saga.status() == OrderSagaStatus.RUNNING) {
            return saga.updatedAt().isBefore(staleBefore);
        }
        return (saga.status() == OrderSagaStatus.COMPENSATING || saga.status() == OrderSagaStatus.MANUAL_REVIEW)
                && saga.nextRetryAt() != null && !saga.nextRetryAt().isAfter(retryBefore);
    }
}
