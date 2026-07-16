package com.emall.identity;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryIdentityLifecycleRepository implements IdentityLifecycleRepository {
    private final ConcurrentMap<Long, IdentityLifecycle> lifecycles = new ConcurrentHashMap<>();

    @Override
    public IdentityLifecycle save(IdentityLifecycle lifecycle) {
        lifecycles.put(lifecycle.accountId(), lifecycle);
        return lifecycle;
    }

    @Override
    public Optional<IdentityLifecycle> findByAccountId(long accountId) {
        return Optional.ofNullable(lifecycles.get(accountId));
    }

    @Override
    public Optional<IdentityLifecycle> findByAccountIdForUpdate(long accountId) {
        return findByAccountId(accountId);
    }

    @Override
    public Optional<IdentityLifecycle> findByRegistrationId(String registrationId) {
        return lifecycles.values().stream().filter(value -> value.registrationId().equals(registrationId)).findFirst();
    }

    @Override
    public List<IdentityLifecycle> findDueForReconciliation(int partition, Instant dueBefore, int limit) {
        return lifecycles.values().stream().filter(value -> value.reconciliationPartition() == partition)
                .filter(value -> value.projectionStatus() != ProfileProjectionStatus.DELETED)
                .filter(value -> !value.nextReconcileAt().isAfter(dueBefore)).sorted(Comparator
                        .comparing(IdentityLifecycle::nextReconcileAt).thenComparingLong(IdentityLifecycle::accountId))
                .limit(limit).toList();
    }
}
