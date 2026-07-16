package com.emall.identity;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class AccountLifecycleReconciler {
    private final AccountLifecycleService lifecycleService;
    private final PartitionedShardWorkCoordinator coordinator;
    private final BusinessMetrics businessMetrics;
    private final int reconciliationPartitions;
    private final int maxPartitionsPerRun;
    private final int batchSize;
    private final Duration leaseTtl;

    AccountLifecycleReconciler(AccountLifecycleService lifecycleService, PartitionedShardWorkCoordinator coordinator,
            BusinessMetrics businessMetrics,
            @Value("${emall.identity.lifecycle.reconciliation-partitions:256}") int reconciliationPartitions,
            @Value("${emall.identity.lifecycle.max-partitions-per-run:16}") int maxPartitionsPerRun,
            @Value("${emall.identity.lifecycle.reconciliation-batch-size:200}") int batchSize,
            @Value("${emall.identity.lifecycle.reconciliation-lease-ttl:30s}") Duration leaseTtl) {
        this.lifecycleService = lifecycleService;
        this.coordinator = coordinator;
        this.businessMetrics = businessMetrics;
        if (reconciliationPartitions <= 0 || reconciliationPartitions > 4_096) {
            throw new IllegalArgumentException("reconciliation partitions must be between 1 and 4096");
        }
        this.reconciliationPartitions = reconciliationPartitions;
        this.maxPartitionsPerRun = maxPartitionsPerRun;
        this.batchSize = batchSize;
        this.leaseTtl = leaseTtl;
    }

    @Scheduled(fixedDelayString = "${emall.identity.lifecycle.reconciliation-delay:30s}")
    void reconcile() {
        coordinator.executeLogicalPartitions("identity-account-lifecycle-reconciliation", reconciliationPartitions,
                maxPartitionsPerRun, leaseTtl, lease -> {
                    List<IdentityLifecycle> due =
                            lifecycleService.dueForReconciliation(lease.shardIndex(), Instant.now(), batchSize);
                    businessMetrics.recordGauge(BusinessMetricNames.IDENTITY_LIFECYCLE_DUE, due.size(), "partition",
                            Integer.toString(lease.shardIndex()));
                    int processed = 0;
                    for (IdentityLifecycle lifecycle : due) {
                        lease.requireValid();
                        lifecycleService.reconcile(lifecycle.accountId());
                        processed++;
                    }
                    return processed;
                });
    }
}
