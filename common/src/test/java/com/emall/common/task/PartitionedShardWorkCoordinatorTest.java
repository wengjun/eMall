package com.emall.common.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.sharding.ShardRoutingOperations;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PartitionedShardWorkCoordinatorTest {
    @Test
    void shouldVisitOnlyTheBoundedRoundRobinPartitions() {
        RecordingShardRouting routing = new RecordingShardRouting(1_024);
        RecordingTaskLock lock = new RecordingTaskLock(true);
        try (PartitionedShardWorkCoordinator coordinator = new PartitionedShardWorkCoordinator(routing, lock)) {
            assertThat(coordinator.execute("order.saga.recover", "order_create_saga", 4, Duration.ofSeconds(30),
                    lease -> 1)).isEqualTo(4);
            assertThat(coordinator.execute("order.saga.recover", "order_create_saga", 4, Duration.ofSeconds(30),
                    lease -> 1)).isEqualTo(4);
        }

        assertThat(routing.visitedShards).containsExactly(0, 1, 2, 3, 4, 5, 6, 7);
        assertThat(lock.acquiredNames).containsExactly("order.saga.recover.partition-0",
                "order.saga.recover.partition-1", "order.saga.recover.partition-2", "order.saga.recover.partition-3",
                "order.saga.recover.partition-4", "order.saga.recover.partition-5", "order.saga.recover.partition-6",
                "order.saga.recover.partition-7");
    }

    @Test
    void shouldEventuallyVisitAllOneThousandTwentyFourPartitionsWithoutUnboundedFanOut() {
        RecordingShardRouting routing = new RecordingShardRouting(1_024);
        RecordingTaskLock lock = new RecordingTaskLock(true);
        try (PartitionedShardWorkCoordinator coordinator = new PartitionedShardWorkCoordinator(routing, lock)) {
            for (int round = 0; round < 128; round++) {
                assertThat(coordinator.execute("inventory.expire", "inventory_reservation", 8, Duration.ofSeconds(30),
                        lease -> 1)).isEqualTo(8);
            }
        }

        assertThat(routing.visitedShards).hasSize(1_024).doesNotHaveDuplicates().contains(0, 1_023);
    }

    @Test
    void shouldLeaseLogicalPartitionsWithoutRoutingToPhysicalDatabases() {
        RecordingShardRouting routing = new RecordingShardRouting(1);
        RecordingTaskLock lock = new RecordingTaskLock(true);
        List<Integer> visited = new ArrayList<>();
        try (PartitionedShardWorkCoordinator coordinator = new PartitionedShardWorkCoordinator(routing, lock)) {
            assertThat(coordinator.executeLogicalPartitions("identity.reconcile", 256, 4, Duration.ofSeconds(30),
                    lease -> {
                        visited.add(lease.shardIndex());
                        return 1;
                    })).isEqualTo(4);
        }

        assertThat(visited).containsExactly(0, 1, 2, 3);
        assertThat(routing.visitedShards).isEmpty();
    }

    @Test
    void shouldRenewSlowPartitionAndAbortWhenRenewalIsLost() {
        RecordingShardRouting routing = new RecordingShardRouting(1);
        RecordingTaskLock healthyLock = new RecordingTaskLock(true);
        try (PartitionedShardWorkCoordinator coordinator = new PartitionedShardWorkCoordinator(routing, healthyLock)) {
            assertThat(coordinator.execute("slow", "orders", 1, Duration.ofMillis(300), lease -> {
                sleep(220);
                lease.requireValid();
                return 1;
            })).isEqualTo(1);
        }
        assertThat(healthyLock.renewals.get()).isGreaterThanOrEqualTo(1);

        RecordingTaskLock failedLock = new RecordingTaskLock(false);
        try (PartitionedShardWorkCoordinator coordinator = new PartitionedShardWorkCoordinator(routing, failedLock)) {
            assertThatThrownBy(() -> coordinator.execute("lost", "orders", 1, Duration.ofMillis(300), lease -> {
                sleep(220);
                lease.requireValid();
                return 1;
            })).isInstanceOf(TaskLeaseLostException.class);
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static final class RecordingTaskLock implements DistributedTaskLock {
        private final List<String> acquiredNames = new CopyOnWriteArrayList<>();
        private final AtomicInteger renewals = new AtomicInteger();
        private final boolean renewalResult;

        private RecordingTaskLock(boolean renewalResult) {
            this.renewalResult = renewalResult;
        }

        @Override
        public boolean tryLock(String lockName, Duration ttl) {
            acquiredNames.add(lockName);
            return true;
        }

        @Override
        public boolean renew(String lockName, Duration ttl) {
            renewals.incrementAndGet();
            return renewalResult;
        }

        @Override
        public void unlock(String lockName) {
        }
    }

    private static final class RecordingShardRouting implements ShardRoutingOperations {
        private final int shardCount;
        private final List<Integer> visitedShards = new ArrayList<>();

        private RecordingShardRouting(int shardCount) {
            this.shardCount = shardCount;
        }

        @Override
        public <T> T execute(String logicalTable, long shardKey, Supplier<T> action) {
            return action.get();
        }

        @Override
        public <T> T execute(String logicalTable, String shardKey, Supplier<T> action) {
            return action.get();
        }

        @Override
        public <T> T executePhysicalShard(String logicalTable, int shardIndex, Supplier<T> action) {
            visitedShards.add(shardIndex);
            return action.get();
        }

        @Override
        public int physicalShardCount(String logicalTable) {
            return shardCount;
        }
    }
}
