package com.emall.common.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class SnowflakeIdGeneratorTest {
    private static final long TEST_TIME = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();

    @Test
    void generatesUniqueIdsAcrossTwentyConcurrentWorkers() throws Exception {
        int workers = 20;
        int idsPerWorker = 2_000;
        Set<Long> ids = ConcurrentHashMap.newKeySet(workers * idsPerWorker);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                SnowflakeIdGenerator generator =
                        new SnowflakeIdGenerator(worker, WorkerIdLease.permanent(worker), () -> TEST_TIME, 0L);
                futures.add(executor.submit(() -> {
                    for (int index = 0; index < idsPerWorker; index++) {
                        ids.add(generator.nextId());
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(ids).hasSize(workers * idsPerWorker);
    }

    @Test
    void stopsIssuingIdsImmediatelyAfterLeaseExpires() {
        AtomicBoolean valid = new AtomicBoolean(true);
        WorkerIdLease lease = new WorkerIdLease() {
            @Override
            public long workerId() {
                return 7;
            }

            @Override
            public void assertValid() {
                if (!valid.get()) {
                    throw new IllegalStateException("lease expired");
                }
            }
        };
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7, lease, () -> TEST_TIME, 0L);

        generator.nextId();
        valid.set(false);

        assertThatThrownBy(generator::nextId).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lease expired");
    }

    @Test
    void failsClosedWhenClockRollbackExceedsConfiguredBudget() {
        AtomicInteger call = new AtomicInteger();
        LongSupplier clock = () -> call.getAndIncrement() == 0 ? TEST_TIME + 10 : TEST_TIME;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(8, WorkerIdLease.permanent(8), clock, 2L);

        generator.nextId();

        assertThatThrownBy(generator::nextId).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Clock moved backwards");
    }
}
