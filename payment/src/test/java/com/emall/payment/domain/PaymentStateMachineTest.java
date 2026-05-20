package com.emall.payment.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class PaymentStateMachineTest {
    @Test
    void shouldAllowExpectedPaymentTransitions() {
        assertThatCode(() -> PaymentStateMachine.requireTransition(PaymentStatus.CREATED, PaymentStatus.SUCCEEDED))
                .doesNotThrowAnyException();
        assertThatCode(() -> PaymentStateMachine.requireTransition(PaymentStatus.SUCCEEDED, PaymentStatus.REFUNDING))
                .doesNotThrowAnyException();
        assertThatCode(() -> PaymentStateMachine.requireTransition(PaymentStatus.REFUNDING, PaymentStatus.REFUNDED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectClosedPaymentTransitions() {
        assertThatThrownBy(() -> PaymentStateMachine.requireTransition(PaymentStatus.CLOSED, PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("illegal payment status transition CLOSED -> SUCCEEDED");
    }
}
