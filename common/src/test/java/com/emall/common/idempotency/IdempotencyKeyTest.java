package com.emall.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdempotencyKeyTest {
    @Test
    void shouldNormalizeNamespaceAndOperationInStorageKey() {
        IdempotencyKey key = IdempotencyKey.of(" Order ", " 70001 ", " request-001 ", " CREATE ");

        assertThat(key.namespace()).isEqualTo("order");
        assertThat(key.ownerId()).isEqualTo("70001");
        assertThat(key.requestId()).isEqualTo("request-001");
        assertThat(key.operation()).isEqualTo("create");
        assertThat(key.storageKey()).isEqualTo("order:70001:create:request-001");
    }

    @Test
    void shouldRejectBlankOrOversizedSegments() {
        assertThatThrownBy(() -> IdempotencyKey.of(" ", "70001", "request-001", "create"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("namespace");
        assertThatThrownBy(() -> IdempotencyKey.of("order", "7".repeat(129), "request-001", "create"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ownerId");
    }
}
