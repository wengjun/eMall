package com.emall.identity;

import java.util.List;
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
        return Optional.ofNullable(identityMapper.findAccount(accountId));
    }

    @Override
    public Optional<IdentityAccount> findAccountBySubject(String subject) {
        return Optional.ofNullable(identityMapper.findAccountBySubject(subject));
    }

    @Override
    public DeviceSession saveSession(DeviceSession session) {
        identityMapper.saveSession(session);
        return session;
    }

    @Override
    public Optional<DeviceSession> findSession(long sessionId) {
        return Optional.ofNullable(identityMapper.findSession(sessionId));
    }

    @Override
    public Optional<DeviceSession> findSessionByAccessToken(String accessToken) {
        return Optional.ofNullable(identityMapper.findSessionByAccessToken(accessToken));
    }

    @Override
    public PermissionGrant saveGrant(PermissionGrant grant) {
        identityMapper.saveGrant(grant);
        return grant;
    }

    @Override
    public List<PermissionGrant> findGrants(long accountId) {
        return identityMapper.findGrants(accountId);
    }

    @Override
    public ServiceClient saveServiceClient(ServiceClient client) {
        identityMapper.saveServiceClient(client);
        return client;
    }

    @Override
    public Optional<ServiceClient> findServiceClient(String clientKey) {
        return Optional.ofNullable(identityMapper.findServiceClient(clientKey));
    }

    @Override
    public MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount) {
        identityMapper.saveSubAccount(subAccount);
        return subAccount;
    }

    @Override
    public List<MerchantSubAccount> findSubAccounts(long merchantId) {
        return identityMapper.findSubAccounts(merchantId);
    }
}
