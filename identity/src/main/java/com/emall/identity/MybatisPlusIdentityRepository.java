package com.emall.identity;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusIdentityRepository implements IdentityRepository {
    private final IdentityMapper identityMapper;

    MybatisPlusIdentityRepository(IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    @Override
    public IdentityAccount saveAccount(IdentityAccount account) {
        identityMapper.saveAccount(account);
        return account;
    }

    @Override
    public Optional<IdentityAccount> findAccount(long accountId) {
        return Optional.ofNullable(identityMapper.findAccount(accountId)).map(this::mapAccount);
    }

    @Override
    public Optional<IdentityAccount> findAccountBySubject(String subject) {
        return Optional.ofNullable(identityMapper.findAccountBySubject(subject)).map(this::mapAccount);
    }

    @Override
    public DeviceSession saveSession(DeviceSession session) {
        identityMapper.saveSession(session);
        return session;
    }

    @Override
    public Optional<DeviceSession> findSession(long sessionId) {
        return Optional.ofNullable(identityMapper.findSession(sessionId)).map(this::mapSession);
    }

    @Override
    public PermissionGrant saveGrant(PermissionGrant grant) {
        identityMapper.saveGrant(grant);
        return grant;
    }

    @Override
    public List<PermissionGrant> findGrants(long accountId) {
        return identityMapper.findGrants(accountId).stream().map(this::mapGrant).toList();
    }

    @Override
    public ServiceClient saveServiceClient(ServiceClient client) {
        identityMapper.saveServiceClient(client);
        return client;
    }

    @Override
    public Optional<ServiceClient> findServiceClient(String clientKey) {
        return Optional.ofNullable(identityMapper.findServiceClient(clientKey)).map(this::mapClient);
    }

    @Override
    public MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount) {
        identityMapper.saveSubAccount(subAccount);
        return subAccount;
    }

    @Override
    public List<MerchantSubAccount> findSubAccounts(long merchantId) {
        return identityMapper.findSubAccounts(merchantId).stream().map(this::mapSubAccount).toList();
    }

    private IdentityAccount mapAccount(Map<String, Object> row) {
        return new IdentityAccount(longValue(row, "account_id"),
                IdentityType.valueOf(stringValue(row, "identity_type")), stringValue(row, "subject"),
                stringValue(row, "display_name"), IdentityStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private DeviceSession mapSession(Map<String, Object> row) {
        return new DeviceSession(longValue(row, "session_id"), longValue(row, "account_id"),
                stringValue(row, "device_id"), stringValue(row, "access_token"), stringValue(row, "refresh_token"),
                SessionStatus.valueOf(stringValue(row, "status")), instantValue(row, "expires_at"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private PermissionGrant mapGrant(Map<String, Object> row) {
        return new PermissionGrant(longValue(row, "grant_id"), longValue(row, "account_id"),
                stringValue(row, "scope"), stringValue(row, "resource"), instantValue(row, "created_at"));
    }

    private ServiceClient mapClient(Map<String, Object> row) {
        return new ServiceClient(longValue(row, "client_id"), stringValue(row, "client_key"),
                stringValue(row, "secret_hash"), stringValue(row, "scopes"), booleanValue(row, "active"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private MerchantSubAccount mapSubAccount(Map<String, Object> row) {
        return new MerchantSubAccount(longValue(row, "sub_account_id"), longValue(row, "merchant_id"),
                longValue(row, "account_id"), stringValue(row, "role_code"), booleanValue(row, "active"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}
