package com.emall.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface IdentityLifecycleMapper extends BaseMapper<IdentityLifecycle> {
    @Select("""
            SELECT account_id, registration_id, request_fingerprint, binding_hash, projection_status,
                   last_published_version, last_acknowledged_version, reconciliation_partition,
                   next_reconcile_at, created_at, updated_at
            FROM identity_lifecycle
            WHERE account_id = #{accountId}
            FOR UPDATE
            """)
    IdentityLifecycle findByAccountIdForUpdate(@Param("accountId") long accountId);
}
