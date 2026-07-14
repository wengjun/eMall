package com.emall.common.id;

public interface WorkerIdLease extends AutoCloseable {
    long workerId();

    void assertValid();

    @Override
    default void close() {
    }

    static WorkerIdLease permanent(long workerId) {
        return new WorkerIdLease() {
            @Override
            public long workerId() {
                return workerId;
            }

            @Override
            public void assertValid() {
            }
        };
    }
}
