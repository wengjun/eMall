package com.emall.payment.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.task.PartitionedShardWorkCoordinator.PartitionLease;
import com.emall.payment.service.PaymentService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentCompensationJobTest {
    @Test
    void shouldRouteEveryRecoveryFlowThroughBoundedPartitionWork() {
        PaymentService paymentService = mock(PaymentService.class);
        PartitionedShardWorkCoordinator workCoordinator = mock(PartitionedShardWorkCoordinator.class);
        PartitionLease lease = mock(PartitionLease.class);
        when(workCoordinator.execute(any(), any(), anyInt(), any(Duration.class), any()))
                .thenAnswer(invocation -> invocation
                        .getArgument(4, PartitionedShardWorkCoordinator.ShardPartitionTask.class).run(lease));
        when(paymentService.findSucceededButUnconfirmed(13)).thenReturn(List.of());
        when(paymentService.findUnreconciledStatements(13)).thenReturn(List.of());
        when(paymentService.findCreatedRefunds(13)).thenReturn(List.of());
        when(paymentService.findProcessingRefunds(13)).thenReturn(List.of());
        PaymentCompensationJob job =
                new PaymentCompensationJob(paymentService, workCoordinator, 8, Duration.ofSeconds(30));

        assertThat(job.retryOrderConfirmation(100)).isZero();
        assertThat(job.reconcileChannelStatements(100)).isZero();
        assertThat(job.processRefunds(100)).isZero();
        verify(workCoordinator).execute(eq("payment.compensation.retry-order-confirmation"), eq("payment_order"), eq(8),
                eq(Duration.ofSeconds(30)), any());
        verify(workCoordinator).execute(eq("payment.reconciliation.channel-statements"),
                eq("payment_channel_statement"), eq(8), eq(Duration.ofSeconds(30)), any());
        verify(workCoordinator).execute(eq("payment.compensation.refresh-processing-refunds"),
                eq("payment_refund_order"), eq(8), eq(Duration.ofSeconds(30)), any());
    }
}
