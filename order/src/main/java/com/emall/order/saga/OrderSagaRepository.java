package com.emall.order.saga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderSagaRepository {
    OrderCreateSaga save(OrderCreateSaga saga);

    Optional<OrderCreateSaga> findByRequestId(String requestId);

    List<OrderCreateSaga> findRecoverable(Instant staleBefore, Instant retryBefore, int limit);
}
