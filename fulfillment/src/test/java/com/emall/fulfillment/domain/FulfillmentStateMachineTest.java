package com.emall.fulfillment.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class FulfillmentStateMachineTest {
    @Test
    void shouldAllowExpectedFulfillmentTransitions() {
        assertThatCode(
                () -> FulfillmentStateMachine.requireTransition(ShipmentStatus.ALLOCATED, ShipmentStatus.SHIPPED))
                .doesNotThrowAnyException();
        assertThatCode(
                () -> FulfillmentStateMachine.requireTransition(ShipmentStatus.SHIPPED, ShipmentStatus.DELIVERED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDeliveredToCancelledTransition() {
        assertThatThrownBy(
                () -> FulfillmentStateMachine.requireTransition(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("illegal fulfillment status transition DELIVERED -> CANCELLED");
    }
}
