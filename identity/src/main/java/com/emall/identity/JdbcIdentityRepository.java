package com.emall.identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcIdentityRepository implements IdentityRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcIdentityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IdentityAccount saveAccount(IdentityAccount account) {
        jdbcTemplate.update("""
                INSERT INTO identity_account
                    (account_id, identity_type, subject, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, account.accountId(), account.type().name(), account.subject(), account.displayName(),
                account.status().name(), Timestamp.from(account.createdAt()), Timestamp.from(account.updatedAt()));
        return account;
    }

    @Override
    public Optional<IdentityAccount> findAccount(long accountId) {
        return jdbcTemplate.query("SELECT * FROM identity_account WHERE account_id = ?", this::mapAccount, accountId)
                .stream().findFirst();
    }

    @Override
    public Optional<IdentityAccount> findAccountBySubject(String subject) {
        return jdbcTemplate.query("SELECT * FROM identity_account WHERE subject = ?", this::mapAccount, subject)
                .stream().findFirst();
    }

    @Override
    public DeviceSession saveSession(DeviceSession session) {
        jdbcTemplate.update("""
                INSERT INTO identity_device_session
                    (session_id, account_id, device_id, access_token, refresh_token, status, expires_at,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, session.sessionId(), session.accountId(), session.deviceId(), session.accessToken(),
                session.refreshToken(), session.status().name(), Timestamp.from(session.expiresAt()),
                Timestamp.from(session.createdAt()), Timestamp.from(session.updatedAt()));
        return session;
    }

    @Override
    public Optional<DeviceSession> findSession(long sessionId) {
        return jdbcTemplate
                .query("SELECT * FROM identity_device_session WHERE session_id = ?", this::mapSession, sessionId)
                .stream().findFirst();
    }

    @Override
    public PermissionGrant saveGrant(PermissionGrant grant) {
        jdbcTemplate.update("""
                INSERT INTO identity_permission_grant (grant_id, account_id, scope, resource, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, grant.grantId(), grant.accountId(), grant.scope(), grant.resource(),
                Timestamp.from(grant.createdAt()));
        return grant;
    }

    @Override
    public List<PermissionGrant> findGrants(long accountId) {
        return jdbcTemplate.query("SELECT * FROM identity_permission_grant WHERE account_id = ?", this::mapGrant,
                accountId);
    }

    @Override
    public ServiceClient saveServiceClient(ServiceClient client) {
        jdbcTemplate.update("""
                INSERT INTO identity_service_client
                    (client_id, client_key, secret_hash, scopes, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE scopes = VALUES(scopes), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """, client.clientId(), client.clientKey(), client.secretHash(), client.scopes(), client.active(),
                Timestamp.from(client.createdAt()), Timestamp.from(client.updatedAt()));
        return client;
    }

    @Override
    public Optional<ServiceClient> findServiceClient(String clientKey) {
        return jdbcTemplate
                .query("SELECT * FROM identity_service_client WHERE client_key = ?", this::mapClient, clientKey)
                .stream().findFirst();
    }

    @Override
    public MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount) {
        jdbcTemplate.update("""
                INSERT INTO identity_merchant_sub_account
                    (sub_account_id, merchant_id, account_id, role_code, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE role_code = VALUES(role_code), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """, subAccount.subAccountId(), subAccount.merchantId(), subAccount.accountId(), subAccount.roleCode(),
                subAccount.active(), Timestamp.from(subAccount.createdAt()), Timestamp.from(subAccount.updatedAt()));
        return subAccount;
    }

    @Override
    public List<MerchantSubAccount> findSubAccounts(long merchantId) {
        return jdbcTemplate.query("SELECT * FROM identity_merchant_sub_account WHERE merchant_id = ?",
                this::mapSubAccount, merchantId);
    }

    private IdentityAccount mapAccount(ResultSet rs, int rowNum) throws SQLException {
        return new IdentityAccount(rs.getLong("account_id"), IdentityType.valueOf(rs.getString("identity_type")),
                rs.getString("subject"), rs.getString("display_name"), IdentityStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private DeviceSession mapSession(ResultSet rs, int rowNum) throws SQLException {
        return new DeviceSession(rs.getLong("session_id"), rs.getLong("account_id"), rs.getString("device_id"),
                rs.getString("access_token"), rs.getString("refresh_token"),
                SessionStatus.valueOf(rs.getString("status")), rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private PermissionGrant mapGrant(ResultSet rs, int rowNum) throws SQLException {
        return new PermissionGrant(rs.getLong("grant_id"), rs.getLong("account_id"), rs.getString("scope"),
                rs.getString("resource"), rs.getTimestamp("created_at").toInstant());
    }

    private ServiceClient mapClient(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceClient(rs.getLong("client_id"), rs.getString("client_key"), rs.getString("secret_hash"),
                rs.getString("scopes"), rs.getBoolean("active"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private MerchantSubAccount mapSubAccount(ResultSet rs, int rowNum) throws SQLException {
        return new MerchantSubAccount(rs.getLong("sub_account_id"), rs.getLong("merchant_id"), rs.getLong("account_id"),
                rs.getString("role_code"), rs.getBoolean("active"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
