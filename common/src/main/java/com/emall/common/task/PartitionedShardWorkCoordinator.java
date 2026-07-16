package com.emall.common.task;

import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionedShardWorkCoordinator implements AutoCloseable {
    private final ShardRoutingOperations shardRoutingOperations;
    private final DistributedTaskLock taskLock;
    private final BusinessMetrics businessMetrics;
    private final ScheduledExecutorService heartbeatExecutor;
    private final ConcurrentMap<String, AtomicInteger> taskCursors = new ConcurrentHashMap<>();

    public PartitionedShardWorkCoordinator(ShardRoutingOperations shardRoutingOperations,
            DistributedTaskLock taskLock) {
        this(shardRoutingOperations, taskLock, BusinessMetrics.noop(),
                Executors.newSingleThreadScheduledExecutor(new HeartbeatThreadFactory()));
    }

    public PartitionedShardWorkCoordinator(ShardRoutingOperations shardRoutingOperations, DistributedTaskLock taskLock,
            BusinessMetrics businessMetrics) {
        this(shardRoutingOperations, taskLock, businessMetrics,
                Executors.newSingleThreadScheduledExecutor(new HeartbeatThreadFactory()));
    }

    PartitionedShardWorkCoordinator(ShardRoutingOperations shardRoutingOperations, DistributedTaskLock taskLock,
            ScheduledExecutorService heartbeatExecutor) {
        this(shardRoutingOperations, taskLock, BusinessMetrics.noop(), heartbeatExecutor);
    }

    private PartitionedShardWorkCoordinator(ShardRoutingOperations shardRoutingOperations, DistributedTaskLock taskLock,
            BusinessMetrics businessMetrics, ScheduledExecutorService heartbeatExecutor) {
        this.shardRoutingOperations = Objects.requireNonNull(shardRoutingOperations);
        this.taskLock = Objects.requireNonNull(taskLock);
        this.businessMetrics = Objects.requireNonNull(businessMetrics);
        this.heartbeatExecutor = Objects.requireNonNull(heartbeatExecutor);
    }

    public int execute(String taskName, String logicalTable, int maxPartitions, Duration leaseTtl,
            ShardPartitionTask task) {
        validate(taskName, logicalTable, maxPartitions, leaseTtl, task);
        int shardCount = shardRoutingOperations.physicalShardCount(logicalTable);
        return executePartitions(taskName, logicalTable, shardCount, maxPartitions, leaseTtl, task);
    }

    public int executeLogicalPartitions(String taskName, int partitionCount, int maxPartitions, Duration leaseTtl,
            ShardPartitionTask task) {
        validateLogical(taskName, partitionCount, maxPartitions, leaseTtl, task);
        return executePartitions(taskName, null, partitionCount, maxPartitions, leaseTtl, task);
    }

    private int executePartitions(String taskName, String logicalTable, int partitionCount, int maxPartitions,
            Duration leaseTtl, ShardPartitionTask task) {
        int partitionsToVisit = Math.min(partitionCount, maxPartitions);
        int start = Math.floorMod(
                taskCursors.computeIfAbsent(taskName, ignored -> new AtomicInteger()).getAndAdd(partitionsToVisit),
                partitionCount);
        int processed = 0;
        for (int offset = 0; offset < partitionsToVisit; offset++) {
            int partition = Math.floorMod(start + offset, partitionCount);
            processed += executePartition(taskName, logicalTable, partition, leaseTtl, task);
        }
        return processed;
    }

    private int executePartition(String taskName, String logicalTable, int shardIndex, Duration leaseTtl,
            ShardPartitionTask task) {
        String lockName = taskName + ".partition-" + shardIndex;
        if (!taskLock.tryLock(lockName, leaseTtl)) {
            businessMetrics.increment(BusinessMetricNames.TASK_PARTITION_BUSY, "task", taskName);
            return 0;
        }
        businessMetrics.increment(BusinessMetricNames.TASK_PARTITION_ACQUIRED, "task", taskName);
        AtomicBoolean valid = new AtomicBoolean(true);
        long heartbeatMillis = Math.max(100L, leaseTtl.toMillis() / 3L);
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (!taskLock.renew(lockName, leaseTtl) && valid.compareAndSet(true, false)) {
                    businessMetrics.increment(BusinessMetricNames.TASK_PARTITION_LEASE_LOST, "task", taskName);
                }
            } catch (RuntimeException exception) {
                if (valid.compareAndSet(true, false)) {
                    businessMetrics.increment(BusinessMetricNames.TASK_PARTITION_LEASE_LOST, "task", taskName);
                }
            }
        }, heartbeatMillis, heartbeatMillis, TimeUnit.MILLISECONDS);
        try {
            PartitionLease lease = new PartitionLease(lockName, shardIndex, valid);
            int result = logicalTable == null
                    ? task.run(lease)
                    : shardRoutingOperations.executePhysicalShard(logicalTable, shardIndex, () -> task.run(lease));
            lease.requireValid();
            businessMetrics.recordGauge("emall_task_partition_batch_size", result, "task", taskName, "partition",
                    Integer.toString(shardIndex));
            return result;
        } finally {
            heartbeat.cancel(false);
            taskLock.unlock(lockName);
        }
    }

    private void validate(String taskName, String logicalTable, int maxPartitions, Duration leaseTtl,
            ShardPartitionTask task) {
        if (taskName == null || taskName.isBlank() || logicalTable == null || logicalTable.isBlank()) {
            throw new IllegalArgumentException("task name and logical table are required");
        }
        if (maxPartitions <= 0 || maxPartitions > 128) {
            throw new IllegalArgumentException("max partitions must be between 1 and 128");
        }
        if (leaseTtl == null || leaseTtl.toMillis() < 300) {
            throw new IllegalArgumentException("lease TTL must be at least 300 ms");
        }
        Objects.requireNonNull(task, "task must not be null");
    }

    private void validateLogical(String taskName, int partitionCount, int maxPartitions, Duration leaseTtl,
            ShardPartitionTask task) {
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("task name is required");
        }
        if (partitionCount <= 0 || partitionCount > 4_096) {
            throw new IllegalArgumentException("partition count must be between 1 and 4096");
        }
        if (maxPartitions <= 0 || maxPartitions > 128) {
            throw new IllegalArgumentException("max partitions must be between 1 and 128");
        }
        if (leaseTtl == null || leaseTtl.toMillis() < 300) {
            throw new IllegalArgumentException("lease TTL must be at least 300 ms");
        }
        Objects.requireNonNull(task, "task must not be null");
    }

    @Override
    public void close() {
        heartbeatExecutor.shutdownNow();
    }

    @FunctionalInterface
    public interface ShardPartitionTask {
        int run(PartitionLease lease);
    }

    public static final class PartitionLease {
        private final String lockName;
        private final int shardIndex;
        private final AtomicBoolean valid;

        private PartitionLease(String lockName, int shardIndex, AtomicBoolean valid) {
            this.lockName = lockName;
            this.shardIndex = shardIndex;
            this.valid = valid;
        }

        public int shardIndex() {
            return shardIndex;
        }

        public boolean isValid() {
            return valid.get();
        }

        public void requireValid() {
            if (!isValid()) {
                throw new TaskLeaseLostException(lockName);
            }
        }
    }

    private static final class HeartbeatThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "emall-task-lease-heartbeat");
            thread.setDaemon(true);
            return thread;
        }
    }
}
