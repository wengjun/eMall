package com.emall.identity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
