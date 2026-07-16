package com.emall.identity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.persistence.BoundedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusIdentityLifecycleRepository implements IdentityLifecycleRepository {
    private final IdentityLifecycleMapper mapper;

    MybatisPlusIdentityLifecycleRepository(IdentityLifecycleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public IdentityLifecycle save(IdentityLifecycle lifecycle) {
        if (mapper.selectById(lifecycle.accountId()) == null) {
            mapper.insert(lifecycle);
        } else {
            mapper.updateById(lifecycle);
        }
        return lifecycle;
    }

    @Override
    public Optional<IdentityLifecycle> findByAccountId(long accountId) {
        return Optional.ofNullable(mapper.selectById(accountId));
    }

    @Override
    public Optional<IdentityLifecycle> findByAccountIdForUpdate(long accountId) {
        return Optional.ofNullable(mapper.findByAccountIdForUpdate(accountId));
    }

    @Override
    public Optional<IdentityLifecycle> findByRegistrationId(String registrationId) {
        return Optional.ofNullable(
                mapper.selectOne(new QueryWrapper<IdentityLifecycle>().eq("registration_id", registrationId)));
    }

    @Override
    public List<IdentityLifecycle> findDueForReconciliation(int partition, Instant dueBefore, int limit) {
        QueryWrapper<IdentityLifecycle> query = new QueryWrapper<IdentityLifecycle>()
                .eq("reconciliation_partition", partition).le("next_reconcile_at", dueBefore)
                .ne("projection_status", ProfileProjectionStatus.DELETED.name()).orderByAsc("next_reconcile_at")
                .orderByAsc("account_id");
        return BoundedQuery.page(mapper, query, limit);
    }
}
