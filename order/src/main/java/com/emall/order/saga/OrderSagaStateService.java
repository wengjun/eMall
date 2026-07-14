package com.emall.order.saga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSagaStateService {
    private final OrderSagaRepository repository;

    public OrderSagaStateService(OrderSagaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderCreateSaga save(OrderCreateSaga saga) {
        return repository.save(saga);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderCreateSaga require(String requestId) {
        return repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("order saga not found: " + requestId));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<OrderCreateSaga> find(String requestId) {
        return repository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<OrderCreateSaga> recoverable(Instant staleBefore, Instant retryBefore, int limit) {
        return repository.findRecoverable(staleBefore, retryBefore, limit);
    }
}
