package com.emall.order.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class OrderLocalTransactionTest {
    @Test
    void shouldBoundOnlyTheLocalActionWithATransactionAndRecordDuration() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);
        when(transactionManagerProvider.getIfAvailable()).thenReturn(transactionManager);
        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);
        OrderLocalTransaction transaction =
                new OrderLocalTransaction(transactionManagerProvider, meterRegistryProvider, 3);

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        String result = transaction.execute("create", () -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return "committed";
        });

        assertThat(result).isEqualTo("committed");
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(meterRegistry.get(OrderLocalTransaction.DURATION_METRIC).tag("operation", "create").timer().count())
                .isEqualTo(1);
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // The abstract manager owns thread-bound synchronization for this synthetic
            // transaction.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // No external resource is used by this boundary test.
        }
    }
}
