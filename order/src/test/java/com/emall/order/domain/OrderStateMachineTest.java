package com.emall.order.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {
    @Test
    void shouldAllowExpectedOrderTransitions() {
        assertThatCode(() -> OrderStateMachine.requireTransition(OrderStatus.CREATED, OrderStatus.PAID))
                .doesNotThrowAnyException();
        assertThatCode(() -> OrderStateMachine.requireTransition(OrderStatus.CREATED, OrderStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> OrderStateMachine.requireTransition(OrderStatus.PENDING_RETRY, OrderStatus.CREATED))
                .doesNotThrowAnyException();
        assertThatCode(() -> OrderStateMachine.requireTransition(OrderStatus.PAID, OrderStatus.CLOSED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTerminalOrderTransitions() {
        assertThatThrownBy(() -> OrderStateMachine.requireTransition(OrderStatus.CANCELLED, OrderStatus.PAID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("illegal order status transition CANCELLED -> PAID");
    }
}
