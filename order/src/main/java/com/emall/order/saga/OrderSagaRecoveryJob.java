package com.emall.order.saga;

import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.task.DistributedTaskLock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaRecoveryJob {
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private final OrderSagaStateService stateService;
    private final OrderSagaCoordinator coordinator;
    private final ShardRoutingOperations shardRoutingOperations;
    private final DistributedTaskLock taskLock;

    public OrderSagaRecoveryJob(OrderSagaStateService stateService, OrderSagaCoordinator coordinator,
            ShardRoutingOperations shardRoutingOperations, DistributedTaskLock taskLock) {
        this.stateService = stateService;
        this.coordinator = coordinator;
        this.shardRoutingOperations = shardRoutingOperations;
        this.taskLock = taskLock;
    }

    @Scheduled(fixedDelay = 10000)
    public void recover() {
        taskLock.executeIfAcquired("order.saga.recover", LOCK_TTL, () -> recover(100));
    }

    public int recover(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        Instant now = Instant.now();
        int shardCount = shardRoutingOperations.physicalShardCount("order_create_saga");
        int perShardLimit = Math.max(1, (boundedLimit + shardCount - 1) / shardCount);
        return shardRoutingOperations
                .executeAll("order_create_saga",
                        () -> stateService.recoverable(now.minus(Duration.ofMinutes(5)), now, perShardLimit))
                .stream().flatMap(java.util.List::stream).limit(boundedLimit).map(saga -> {
                    coordinator.recover(saga);
                    return saga;
                }).toList().size();
    }
}
