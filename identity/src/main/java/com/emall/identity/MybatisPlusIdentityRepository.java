package com.emall.identity;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.persistence.BoundedQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusIdentityRepository implements IdentityRepository {
    private final IdentityMapper identityMapper;
    private final IdentityAccountMapper accountMapper;
    private final DeviceSessionMapper sessionMapper;
    private final IdentityCredentialMapper credentialMapper;
    private final PermissionGrantMapper grantMapper;
    private final ServiceClientMapper serviceClientMapper;
    private final MerchantSubAccountMapper subAccountMapper;

    MybatisPlusIdentityRepository(IdentityMapper identityMapper, IdentityAccountMapper accountMapper,
            DeviceSessionMapper sessionMapper, IdentityCredentialMapper credentialMapper,
            PermissionGrantMapper grantMapper, ServiceClientMapper serviceClientMapper,
            MerchantSubAccountMapper subAccountMapper) {
        this.identityMapper = identityMapper;
        this.accountMapper = accountMapper;
        this.sessionMapper = sessionMapper;
        this.credentialMapper = credentialMapper;
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
    public Optional<IdentityAccount> findAccountForUpdate(long accountId) {
        return Optional.ofNullable(identityMapper.findAccountForUpdate(accountId));
    }

    @Override
    public Optional<IdentityAccount> findAccountBySubjectForUpdate(String subject) {
        return Optional.ofNullable(identityMapper.findAccountBySubjectForUpdate(subject));
    }

    @Override
    public boolean transitionAccountStatus(long accountId, IdentityStatus expected, IdentityStatus next,
            java.time.Instant updatedAt) {
        return identityMapper.transitionAccountStatus(accountId, expected, next, updatedAt) == 1;
    }

    @Override
    public boolean eraseAccount(long accountId, IdentityStatus expected, java.time.Instant updatedAt) {
        return identityMapper.eraseAccount(accountId, expected, updatedAt) == 1;
    }

    @Override
    public IdentityCredential saveCredential(IdentityCredential credential) {
        if (credentialMapper.selectById(credential.accountId()) == null) {
            credentialMapper.insert(credential);
        } else {
            credentialMapper.updateById(credential);
        }
        return credential;
    }

    @Override
    public Optional<IdentityCredential> findCredential(long accountId) {
        return Optional.ofNullable(credentialMapper.selectById(accountId));
    }

    @Override
    public void deleteCredential(long accountId) {
        identityMapper.deleteCredential(accountId);
    }

    @Override
    public void recordCredentialFailure(long accountId, java.time.Instant now, java.time.Instant lockedUntil,
            int maximumAttempts) {
        credentialMapper.recordFailure(accountId, now, lockedUntil, maximumAttempts);
    }

    @Override
    public void clearCredentialFailures(long accountId, java.time.Instant now) {
        credentialMapper.clearFailures(accountId, now);
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
        return Optional
                .ofNullable(sessionMapper.selectOne(new QueryWrapper<DeviceSession>().eq("access_token", accessToken)));
    }

    @Override
    public Optional<DeviceSession> findSessionByRefreshToken(String refreshToken) {
        return Optional.ofNullable(
                sessionMapper.selectOne(new QueryWrapper<DeviceSession>().eq("refresh_token", refreshToken)));
    }

    @Override
    public boolean revokeSessionIfActive(long sessionId, String refreshToken, java.time.Instant updatedAt) {
        return identityMapper.revokeSessionIfActive(sessionId, refreshToken, updatedAt) == 1;
    }

    @Override
    public int revokeAllSessions(long accountId, java.time.Instant updatedAt) {
        return identityMapper.revokeAllSessions(accountId, updatedAt);
    }

    @Override
    public List<DeviceSession> findActiveSessionsForRevocation(long accountId, long afterSessionId,
            java.time.Instant createdAfter, int limit) {
        return identityMapper.findActiveSessionsForRevocation(accountId, afterSessionId, createdAfter,
                BoundedQuery.limit(limit));
    }

    @Override
    public PermissionGrant saveGrant(PermissionGrant grant) {
        grantMapper.insert(grant);
        return grant;
    }

    @Override
    public List<PermissionGrant> findGrants(long accountId) {
        return BoundedQuery.firstPage(grantMapper, new QueryWrapper<PermissionGrant>().eq("account_id", accountId));
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
        return BoundedQuery.firstPage(subAccountMapper,
                new QueryWrapper<MerchantSubAccount>().eq("merchant_id", merchantId));
    }
}
