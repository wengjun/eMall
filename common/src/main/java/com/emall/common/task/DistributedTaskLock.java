package com.emall.common.task;

import java.time.Duration;
import java.util.function.IntSupplier;

public interface DistributedTaskLock {
    boolean tryLock(String lockName, Duration ttl);

    void unlock(String lockName);

    default int executeIfAcquired(String lockName, Duration ttl, IntSupplier task) {
        if (!tryLock(lockName, ttl)) {
            return 0;
        }
        try {
            return task.getAsInt();
        } finally {
            unlock(lockName);
        }
    }
}
