package com.emall.order.saga;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.task.DistributedTaskLock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaRecoveryJob {
    private static final String TASK_NAME = "order.saga.recover";
    private final OrderSagaStateService stateService;
    private final OrderSagaCoordinator coordinator;
    private final PartitionedShardWorkCoordinator workCoordinator;
    private final DistributedTaskLock sagaLease;
    private final int maxPartitionsPerRun;
    private final Duration leaseTtl;

    public OrderSagaRecoveryJob(OrderSagaStateService stateService, OrderSagaCoordinator coordinator,
            PartitionedShardWorkCoordinator workCoordinator, DistributedTaskLock sagaLease,
            @Value("${emall.jobs.saga-recovery.max-partitions-per-run:8}") int maxPartitionsPerRun,
            @Value("${emall.jobs.saga-recovery.lease-ttl:30s}") Duration leaseTtl) {
        this.stateService = stateService;
        this.coordinator = coordinator;
        this.workCoordinator = workCoordinator;
        this.sagaLease = sagaLease;
        this.maxPartitionsPerRun = maxPartitionsPerRun;
        this.leaseTtl = leaseTtl;
    }

    @Scheduled(fixedDelay = 10000)
    public void recover() {
        recover(100);
    }

    public int recover(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int perPartitionLimit = Math.max(1, (boundedLimit + maxPartitionsPerRun - 1) / maxPartitionsPerRun);
        AtomicInteger remaining = new AtomicInteger(boundedLimit);
        Instant now = Instant.now();
        return workCoordinator.execute(TASK_NAME, "order_create_saga", maxPartitionsPerRun, leaseTtl, lease -> {
            int requested = Math.min(perPartitionLimit, remaining.get());
            if (requested <= 0) {
                return 0;
            }
            int processed = 0;
            for (OrderCreateSaga saga : stateService.recoverable(now.minus(Duration.ofMinutes(5)), now, requested)) {
                lease.requireValid();
                if (remaining.get() <= 0) {
                    break;
                }
                String sagaLock = TASK_NAME + ":saga:" + saga.sagaId();
                if (sagaLease.tryLock(sagaLock, leaseTtl)) {
                    try {
                        lease.requireValid();
                        remaining.decrementAndGet();
                        coordinator.recover(saga);
                        processed++;
                    } finally {
                        sagaLease.unlock(sagaLock);
                    }
                }
            }
            return processed;
        });
    }
}
