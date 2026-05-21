package com.emall.identity;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusIdentityRepository implements IdentityRepository {
    private final IdentityMapper identityMapper;
    private final IdentityAccountMapper accountMapper;
    private final DeviceSessionMapper sessionMapper;
    private final PermissionGrantMapper grantMapper;
    private final ServiceClientMapper serviceClientMapper;
    private final MerchantSubAccountMapper subAccountMapper;

    MybatisPlusIdentityRepository(IdentityMapper identityMapper, IdentityAccountMapper accountMapper,
            DeviceSessionMapper sessionMapper, PermissionGrantMapper grantMapper,
            ServiceClientMapper serviceClientMapper, MerchantSubAccountMapper subAccountMapper) {
        this.identityMapper = identityMapper;
        this.accountMapper = accountMapper;
        this.sessionMapper = sessionMapper;
        this.grantMapper = grantMapper;
        this.serviceClientMapper = serviceClientMapper;
        this.subAccountMapper = subAccountMapper;
    }

    @Override
    public IdentityAccount saveAccount(IdentityAccount account) {
        identityMapper.saveAccount(account);
        return account;
    }

    @Override
    public Optional<IdentityAccount> findAccount(long accountId) {
        return Optional.ofNullable(accountMapper.selectById(accountId));
    }

    @Override
    public Optional<IdentityAccount> findAccountBySubject(String subject) {
        return Optional.ofNullable(accountMapper.selectOne(new QueryWrapper<IdentityAccount>().eq("subject", subject)));
    }

    @Override
    public DeviceSession saveSession(DeviceSession session) {
        identityMapper.saveSession(session);
        return session;
    }

    @Override
    public Optional<DeviceSession> findSession(long sessionId) {
        return Optional.ofNullable(sessionMapper.selectById(sessionId));
    }

    @Override
    public Optional<DeviceSession> findSessionByAccessToken(String accessToken) {
        return Optional.ofNullable(
                sessionMapper.selectOne(new QueryWrapper<DeviceSession>().eq("access_token", accessToken)));
    }

    @Override
    public PermissionGrant saveGrant(PermissionGrant grant) {
        grantMapper.insert(grant);
        return grant;
    }

    @Override
    public List<PermissionGrant> findGrants(long accountId) {
        return grantMapper.selectList(new QueryWrapper<PermissionGrant>().eq("account_id", accountId));
    }

    @Override
    public ServiceClient saveServiceClient(ServiceClient client) {
        identityMapper.saveServiceClient(client);
        return client;
    }

    @Override
    public Optional<ServiceClient> findServiceClient(String clientKey) {
        return Optional.ofNullable(
                serviceClientMapper.selectOne(new QueryWrapper<ServiceClient>().eq("client_key", clientKey)));
    }

    @Override
    public MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount) {
        identityMapper.saveSubAccount(subAccount);
        return subAccount;
    }

    @Override
    public List<MerchantSubAccount> findSubAccounts(long merchantId) {
        return subAccountMapper.selectList(new QueryWrapper<MerchantSubAccount>().eq("merchant_id", merchantId));
    }
}
