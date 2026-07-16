package com.emall.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.task.PartitionedShardWorkCoordinator.PartitionLease;
import com.emall.common.task.DistributedTaskLock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderSagaRecoveryJobTest {
    @Test
    void shouldUseBoundedPartitionLeaseAndRespectGlobalBatchLimit() {
        OrderSagaStateService stateService = mock(OrderSagaStateService.class);
        OrderSagaCoordinator sagaCoordinator = mock(OrderSagaCoordinator.class);
        PartitionedShardWorkCoordinator workCoordinator = mock(PartitionedShardWorkCoordinator.class);
        DistributedTaskLock sagaLease = mock(DistributedTaskLock.class);
        PartitionLease lease = mock(PartitionLease.class);
        OrderCreateSaga first = OrderCreateSaga.start(1L, "request-1", 101L, 201L, 301L, Instant.now());
        OrderCreateSaga second = OrderCreateSaga.start(2L, "request-2", 102L, 202L, 302L, Instant.now());
        when(stateService.recoverable(any(), any(), eq(1))).thenReturn(List.of(first, second));
        when(workCoordinator.execute(eq("order.saga.recover"), eq("order_create_saga"), eq(8),
                eq(Duration.ofSeconds(30)), any()))
                .thenAnswer(invocation -> invocation
                        .getArgument(4, PartitionedShardWorkCoordinator.ShardPartitionTask.class).run(lease));
        when(sagaLease.tryLock("order.saga.recover:saga:1", Duration.ofSeconds(30))).thenReturn(true);
        OrderSagaRecoveryJob job = new OrderSagaRecoveryJob(stateService, sagaCoordinator, workCoordinator, sagaLease,
                8, Duration.ofSeconds(30));

        assertThat(job.recover(1)).isEqualTo(1);
        verify(lease, atLeastOnce()).requireValid();
        verify(sagaCoordinator).recover(first);
        verify(sagaCoordinator, never()).recover(second);
        verify(sagaLease).unlock("order.saga.recover:saga:1");
    }
}
