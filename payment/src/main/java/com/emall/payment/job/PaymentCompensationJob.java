package com.emall.payment.job;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.task.PartitionedShardWorkCoordinator.PartitionLease;
import com.emall.payment.service.PaymentService;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompensationJob {
    private final PaymentService paymentService;
    private final PartitionedShardWorkCoordinator workCoordinator;
    private final int maxPartitionsPerRun;
    private final Duration leaseTtl;

    public PaymentCompensationJob(PaymentService paymentService, PartitionedShardWorkCoordinator workCoordinator,
            @Value("${emall.jobs.payment-compensation.max-partitions-per-run:8}") int maxPartitionsPerRun,
            @Value("${emall.jobs.payment-compensation.lease-ttl:30s}") Duration leaseTtl) {
        this.paymentService = paymentService;
        this.workCoordinator = workCoordinator;
        this.maxPartitionsPerRun = maxPartitionsPerRun;
        this.leaseTtl = leaseTtl;
    }

    @Scheduled(fixedDelay = 5000)
    public void retryOrderConfirmation() {
        retryOrderConfirmation(100);
    }

    public int retryOrderConfirmation(int limit) {
        return executePartitioned("payment.compensation.retry-order-confirmation", "payment_order", limit,
                this::retryOrderConfirmationInPartition);
    }

    private int retryOrderConfirmationInPartition(int limit, PartitionLease lease) {
        int processed = 0;
        for (var payment : paymentService.findSucceededButUnconfirmed(limit)) {
            lease.requireValid();
            paymentService.retryOrderConfirmation(payment.paymentId());
            processed++;
        }
        return processed;
    }

    @Scheduled(fixedDelay = 30000)
    public void reconcileChannelStatements() {
        reconcileChannelStatements(100);
    }

    public int reconcileChannelStatements(int limit) {
        return executePartitioned("payment.reconciliation.channel-statements", "payment_channel_statement", limit,
                this::reconcileChannelStatementsInPartition);
    }

    private int reconcileChannelStatementsInPartition(int limit, PartitionLease lease) {
        int processed = 0;
        for (var statement : paymentService.findUnreconciledStatements(limit)) {
            lease.requireValid();
            paymentService.reconcileStatement(statement.statementId());
            processed++;
        }
        return processed;
    }

    @Scheduled(fixedDelay = 10000)
    public void refreshProcessingRefunds() {
        processRefunds(100);
    }

    public int processRefunds(int limit) {
        return executePartitioned("payment.compensation.refresh-processing-refunds", "payment_refund_order", limit,
                this::processRefundsInPartition);
    }

    private int processRefundsInPartition(int limit, PartitionLease lease) {
        int submitted = 0;
        for (var refund : paymentService.findCreatedRefunds(limit)) {
            lease.requireValid();
            paymentService.submitRefund(refund.refundId());
            submitted++;
        }
        int remaining = Math.max(0, limit - submitted);
        int refreshed = 0;
        if (remaining == 0) {
            return submitted;
        }
        for (var refund : paymentService.findProcessingRefunds(remaining)) {
            lease.requireValid();
            paymentService.refreshRefund(refund.refundId());
            refreshed++;
        }
        return submitted + refreshed;
    }

    private int executePartitioned(String taskName, String logicalTable, int limit, PartitionWork work) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int perPartitionLimit = Math.max(1, (boundedLimit + maxPartitionsPerRun - 1) / maxPartitionsPerRun);
        AtomicInteger remaining = new AtomicInteger(boundedLimit);
        return workCoordinator.execute(taskName, logicalTable, maxPartitionsPerRun, leaseTtl, lease -> {
            int requested = Math.min(perPartitionLimit, remaining.get());
            if (requested <= 0) {
                return 0;
            }
            int processed = work.run(requested, lease);
            remaining.addAndGet(-processed);
            return processed;
        });
    }

    @FunctionalInterface
    private interface PartitionWork {
        int run(int limit, PartitionLease lease);
    }
}
