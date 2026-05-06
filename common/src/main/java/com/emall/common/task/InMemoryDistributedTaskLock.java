package com.emall.common.task;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDistributedTaskLock implements DistributedTaskLock {
    private final ConcurrentMap<String, Lease> leases = new ConcurrentHashMap<>();
    private final Clock clock;
    private final String ownerId;

    public InMemoryDistributedTaskLock(Clock clock, String ownerId) {
        this.clock = clock;
        this.ownerId = ownerId;
    }

    @Override
    public synchronized boolean tryLock(String lockName, Duration ttl) {
        Instant now = clock.instant();
        Lease current = leases.get(lockName);
        if (current != null && current.lockedUntil().isAfter(now) && !current.ownerId().equals(ownerId)) {
            return false;
        }
        leases.put(lockName, new Lease(ownerId, now.plus(ttl)));
        return true;
    }

    @Override
    public synchronized void unlock(String lockName) {
        Lease current = leases.get(lockName);
        if (current != null && current.ownerId().equals(ownerId)) {
            leases.remove(lockName);
        }
    }

    private record Lease(String ownerId, Instant lockedUntil) {
    }
}
