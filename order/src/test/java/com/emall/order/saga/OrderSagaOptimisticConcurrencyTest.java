package com.emall.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderSagaOptimisticConcurrencyTest {
    @Test
    void staleTransitionCannotOverwriteACommittedTransition() {
        InMemoryOrderSagaRepository repository = new InMemoryOrderSagaRepository();
        OrderCreateSaga started =
                repository.save(OrderCreateSaga.start(1L, "request-1", 101L, 201L, 301L, Instant.now()));
        OrderCreateSaga firstWriter = started.advance(OrderSagaStage.VALIDATED, null, "request-1");
        OrderCreateSaga staleWriter = started.advance(OrderSagaStage.COUPON_PLANNED, "coupon-1", "request-1");

        repository.save(firstWriter);

        assertThatThrownBy(() -> repository.save(staleWriter)).isInstanceOf(OrderSagaConcurrencyException.class)
                .hasMessageContaining("expectedVersion=0");
        assertThat(repository.findByRequestId("request-1").orElseThrow()).isEqualTo(firstWriter);
    }

    @Test
    void duplicateStartReturnsTheWinningSagaWithoutOverwritingIt() {
        InMemoryOrderSagaRepository repository = new InMemoryOrderSagaRepository();
        OrderCreateSaga winner =
                repository.save(OrderCreateSaga.start(1L, "request-1", 101L, 201L, 301L, Instant.now()));

        OrderCreateSaga duplicate =
                repository.save(OrderCreateSaga.start(2L, "request-1", 102L, 201L, 301L, Instant.now()));

        assertThat(duplicate).isEqualTo(winner);
        assertThat(repository.findByRequestId("request-1").orElseThrow()).isEqualTo(winner);
    }
}
