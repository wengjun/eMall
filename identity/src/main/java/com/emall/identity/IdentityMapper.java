package com.emall.identity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
interface IdentityMapper {
    @Insert("""
            INSERT INTO identity_account
                (account_id, identity_type, subject, display_name, status, created_at, updated_at)
            VALUES (#{account.accountId}, #{account.type}, #{account.subject}, #{account.displayName},
                #{account.status}, #{account.createdAt}, #{account.updatedAt})
            """)
    int saveAccount(@Param("account") IdentityAccount account);

    @Select("""
            SELECT account_id, identity_type, subject, display_name, status, created_at, updated_at
            FROM identity_account
            WHERE account_id = #{accountId}
            FOR UPDATE
            """)
    IdentityAccount findAccountForUpdate(@Param("accountId") long accountId);

    @Select("""
            SELECT account_id, identity_type, subject, display_name, status, created_at, updated_at
            FROM identity_account
            WHERE subject = #{subject}
            FOR UPDATE
            """)
    IdentityAccount findAccountBySubjectForUpdate(@Param("subject") String subject);

    @Update("""
            UPDATE identity_account
            SET status = #{next}, updated_at = #{updatedAt}
            WHERE account_id = #{accountId} AND status = #{expected}
            """)
    int transitionAccountStatus(@Param("accountId") long accountId, @Param("expected") IdentityStatus expected,
            @Param("next") IdentityStatus next, @Param("updatedAt") java.time.Instant updatedAt);

    @Update("""
            UPDATE identity_account
            SET subject = CONCAT('deleted-', account_id), display_name = 'Deleted account',
                status = 'DELETED', updated_at = #{updatedAt}
            WHERE account_id = #{accountId} AND status = #{expected}
            """)
    int eraseAccount(@Param("accountId") long accountId, @Param("expected") IdentityStatus expected,
            @Param("updatedAt") java.time.Instant updatedAt);

    @Delete("DELETE FROM identity_credential WHERE account_id = #{accountId}")
    int deleteCredential(@Param("accountId") long accountId);

    @Insert("""
            INSERT INTO identity_device_session
                (session_id, account_id, device_id, access_token, refresh_token, status, expires_at,
                created_at, updated_at)
            VALUES (#{session.sessionId}, #{session.accountId}, #{session.deviceId}, #{session.accessToken},
                #{session.refreshToken}, #{session.status}, #{session.expiresAt}, #{session.createdAt},
                #{session.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveSession(@Param("session") DeviceSession session);

    @Update("""
            UPDATE identity_device_session
            SET status = 'REVOKED', updated_at = #{updatedAt}
            WHERE session_id = #{sessionId} AND refresh_token = #{refreshToken} AND status = 'ACTIVE'
            """)
    int revokeSessionIfActive(@Param("sessionId") long sessionId, @Param("refreshToken") String refreshToken,
            @Param("updatedAt") java.time.Instant updatedAt);

    @Update("""
            UPDATE identity_device_session
            SET status = 'REVOKED', updated_at = #{updatedAt}
            WHERE account_id = #{accountId} AND status = 'ACTIVE'
            """)
    int revokeAllSessions(@Param("accountId") long accountId, @Param("updatedAt") java.time.Instant updatedAt);

    @Select("""
            SELECT session_id, account_id, device_id, access_token, refresh_token, status, expires_at,
                   created_at, updated_at
            FROM identity_device_session
            WHERE account_id = #{accountId}
              AND status = 'ACTIVE'
              AND created_at >= #{createdAfter}
              AND session_id > #{afterSessionId}
            ORDER BY session_id
            LIMIT #{limit}
            """)
    java.util.List<DeviceSession> findActiveSessionsForRevocation(@Param("accountId") long accountId,
            @Param("afterSessionId") long afterSessionId, @Param("createdAfter") java.time.Instant createdAfter,
            @Param("limit") int limit);

    @Insert("""
            INSERT INTO identity_service_client
                (client_id, client_key, secret_hash, scopes, active, created_at, updated_at)
            VALUES (#{client.clientId}, #{client.clientKey}, #{client.secretHash}, #{client.scopes},
                #{client.active}, #{client.createdAt}, #{client.updatedAt})
            ON DUPLICATE KEY UPDATE scopes = VALUES(scopes), active = VALUES(active),
                updated_at = VALUES(updated_at)
            """)
    int saveServiceClient(@Param("client") ServiceClient client);

    @Insert("""
            INSERT INTO identity_merchant_sub_account
                (sub_account_id, merchant_id, account_id, role_code, active, created_at, updated_at)
            VALUES (#{subAccount.subAccountId}, #{subAccount.merchantId}, #{subAccount.accountId},
                #{subAccount.roleCode}, #{subAccount.active}, #{subAccount.createdAt},
                #{subAccount.updatedAt})
            ON DUPLICATE KEY UPDATE role_code = VALUES(role_code), active = VALUES(active),
                updated_at = VALUES(updated_at)
            """)
    int saveSubAccount(@Param("subAccount") MerchantSubAccount subAccount);
}
