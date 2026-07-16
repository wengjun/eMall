package com.emall.identity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface IdentityLifecycleRepository {
    IdentityLifecycle save(IdentityLifecycle lifecycle);

    Optional<IdentityLifecycle> findByAccountId(long accountId);

    Optional<IdentityLifecycle> findByAccountIdForUpdate(long accountId);

    Optional<IdentityLifecycle> findByRegistrationId(String registrationId);

    List<IdentityLifecycle> findDueForReconciliation(int partition, Instant dueBefore, int limit);
}
