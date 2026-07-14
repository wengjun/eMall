package com.emall.common.id;

import java.time.Instant;
import java.util.function.LongSupplier;

public final class SnowflakeIdGenerator {
    private static final long CUSTOM_EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private final long workerId;
    private final WorkerIdLease workerIdLease;
    private final LongSupplier timeSource;
    private final long maximumClockRollbackMillis;
    private long lastTimestamp = -1L;
    private long sequence;

    public SnowflakeIdGenerator(long workerId) {
        this(workerId, WorkerIdLease.permanent(workerId), System::currentTimeMillis, 0L);
    }

    SnowflakeIdGenerator(WorkerIdLease workerIdLease, long maximumClockRollbackMillis) {
        this(workerIdLease.workerId(), workerIdLease, System::currentTimeMillis, maximumClockRollbackMillis);
    }

    SnowflakeIdGenerator(long workerId, WorkerIdLease workerIdLease, LongSupplier timeSource,
            long maximumClockRollbackMillis) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
        this.workerIdLease = workerIdLease;
        this.timeSource = timeSource;
        this.maximumClockRollbackMillis = maximumClockRollbackMillis;
    }

    public synchronized long nextId() {
        workerIdLease.assertValid();
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            long rollback = lastTimestamp - timestamp;
            if (rollback > maximumClockRollbackMillis) {
                throw new IllegalStateException("Clock moved backwards by " + rollback + "ms");
            }
            timestamp = waitUntil(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - CUSTOM_EPOCH) << (WORKER_BITS + SEQUENCE_BITS)) | (workerId << SEQUENCE_BITS) | sequence;
    }

    public long workerId() {
        return workerId;
    }

    private long waitNextMillis(long timestamp) {
        long current = currentTimeMillis();
        while (current <= timestamp) {
            Thread.onSpinWait();
            current = currentTimeMillis();
        }
        return current;
    }

    private long waitUntil(long timestamp) {
        long current = currentTimeMillis();
        while (current < timestamp) {
            Thread.onSpinWait();
            current = currentTimeMillis();
        }
        return current;
    }

    private long currentTimeMillis() {
        return timeSource.getAsLong();
    }
}
