package com.emall.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
interface IdentityCredentialMapper extends BaseMapper<IdentityCredential> {
    @Update("""
            UPDATE identity_credential
            SET failed_attempts = failed_attempts + 1,
                locked_until = CASE WHEN failed_attempts + 1 >= #{maximumAttempts} THEN #{lockedUntil}
                    ELSE locked_until END,
                updated_at = #{now}
            WHERE account_id = #{accountId}
            """)
    int recordFailure(@Param("accountId") long accountId, @Param("now") java.time.Instant now,
            @Param("lockedUntil") java.time.Instant lockedUntil, @Param("maximumAttempts") int maximumAttempts);

    @Update("""
            UPDATE identity_credential
            SET failed_attempts = 0, locked_until = NULL, updated_at = #{now}
            WHERE account_id = #{accountId} AND (failed_attempts > 0 OR locked_until IS NOT NULL)
            """)
    int clearFailures(@Param("accountId") long accountId, @Param("now") java.time.Instant now);
}
