package com.emall.identity;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface IdentityMapper {
    @Insert("""
            INSERT INTO identity_account
                (account_id, identity_type, subject, display_name, status, created_at, updated_at)
            VALUES (#{account.accountId}, #{account.type}, #{account.subject}, #{account.displayName},
                #{account.status}, #{account.createdAt}, #{account.updatedAt})
            ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveAccount(@Param("account") IdentityAccount account);

    @Select("SELECT * FROM identity_account WHERE account_id = #{accountId}")
    Map<String, Object> findAccount(@Param("accountId") long accountId);

    @Select("SELECT * FROM identity_account WHERE subject = #{subject}")
    Map<String, Object> findAccountBySubject(@Param("subject") String subject);

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

    @Select("SELECT * FROM identity_device_session WHERE session_id = #{sessionId}")
    Map<String, Object> findSession(@Param("sessionId") long sessionId);

    @Insert("""
            INSERT INTO identity_permission_grant (grant_id, account_id, scope, resource, created_at)
            VALUES (#{grant.grantId}, #{grant.accountId}, #{grant.scope}, #{grant.resource},
                #{grant.createdAt})
            """)
    int saveGrant(@Param("grant") PermissionGrant grant);

    @Select("SELECT * FROM identity_permission_grant WHERE account_id = #{accountId}")
    List<Map<String, Object>> findGrants(@Param("accountId") long accountId);

    @Insert("""
            INSERT INTO identity_service_client
                (client_id, client_key, secret_hash, scopes, active, created_at, updated_at)
            VALUES (#{client.clientId}, #{client.clientKey}, #{client.secretHash}, #{client.scopes},
                #{client.active}, #{client.createdAt}, #{client.updatedAt})
            ON DUPLICATE KEY UPDATE scopes = VALUES(scopes), active = VALUES(active),
                updated_at = VALUES(updated_at)
            """)
    int saveServiceClient(@Param("client") ServiceClient client);

    @Select("SELECT * FROM identity_service_client WHERE client_key = #{clientKey}")
    Map<String, Object> findServiceClient(@Param("clientKey") String clientKey);

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

    @Select("SELECT * FROM identity_merchant_sub_account WHERE merchant_id = #{merchantId}")
    List<Map<String, Object>> findSubAccounts(@Param("merchantId") long merchantId);
}
