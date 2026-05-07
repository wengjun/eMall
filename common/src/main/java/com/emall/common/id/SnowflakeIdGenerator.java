package com.emall.common.id;

import java.time.Instant;

public final class SnowflakeIdGenerator {
    private static final long CUSTOM_EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards by " + (lastTimestamp - timestamp) + "ms");
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

    private long waitNextMillis(long timestamp) {
        long current = currentTimeMillis();
        while (current <= timestamp) {
            current = currentTimeMillis();
        }
        return current;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
