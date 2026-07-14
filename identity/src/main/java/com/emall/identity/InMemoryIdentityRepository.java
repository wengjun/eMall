package com.emall.identity;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryIdentityRepository implements IdentityRepository {
    private final ConcurrentMap<Long, IdentityAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, IdentityCredential> credentials = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PermissionGrant> grants = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ServiceClient> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MerchantSubAccount> subAccounts = new ConcurrentHashMap<>();

    @Override
    public IdentityAccount saveAccount(IdentityAccount account) {
        accounts.put(account.accountId(), account);
        return account;
    }

    @Override
    public Optional<IdentityAccount> findAccount(long accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public Optional<IdentityAccount> findAccountBySubject(String subject) {
        return accounts.values().stream().filter(account -> account.subject().equals(subject)).findFirst();
    }

    @Override
    public IdentityCredential saveCredential(IdentityCredential credential) {
        credentials.put(credential.accountId(), credential);
        return credential;
    }

    @Override
    public Optional<IdentityCredential> findCredential(long accountId) {
        return Optional.ofNullable(credentials.get(accountId));
    }

    @Override
    public void recordCredentialFailure(long accountId, Instant now, Instant lockedUntil, int maximumAttempts) {
        credentials.computeIfPresent(accountId,
                (ignored, credential) -> credential.failed(now, maximumAttempts, lockedUntil));
    }

    @Override
    public void clearCredentialFailures(long accountId, Instant now) {
        credentials.computeIfPresent(accountId, (ignored, credential) -> credential.succeeded(now));
    }

    @Override
    public DeviceSession saveSession(DeviceSession session) {
        sessions.put(session.sessionId(), session);
        return session;
    }

    @Override
    public Optional<DeviceSession> findSession(long sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<DeviceSession> findSessionByAccessToken(String accessToken) {
        return sessions.values().stream().filter(session -> session.accessToken().equals(accessToken)).findFirst();
    }

    @Override
    public Optional<DeviceSession> findSessionByRefreshToken(String refreshToken) {
        return sessions.values().stream().filter(session -> session.refreshToken().equals(refreshToken)).findFirst();
    }

    @Override
    public boolean revokeSessionIfActive(long sessionId, String refreshToken, Instant updatedAt) {
        boolean[] revoked = {false};
        sessions.computeIfPresent(sessionId, (ignored, session) -> {
            if (session.status() == SessionStatus.ACTIVE && session.refreshToken().equals(refreshToken)) {
                revoked[0] = true;
                return new DeviceSession(session.sessionId(), session.accountId(), session.deviceId(),
                        session.accessToken(), session.refreshToken(), SessionStatus.REVOKED, session.expiresAt(),
                        session.createdAt(), updatedAt);
            }
            return session;
        });
        return revoked[0];
    }

    @Override
    public PermissionGrant saveGrant(PermissionGrant grant) {
        grants.put(grant.grantId(), grant);
        return grant;
    }

    @Override
    public List<PermissionGrant> findGrants(long accountId) {
        return grants.values().stream().filter(grant -> grant.accountId() == accountId).toList();
    }

    @Override
    public ServiceClient saveServiceClient(ServiceClient client) {
        clients.put(client.clientKey(), client);
        return client;
    }

    @Override
    public Optional<ServiceClient> findServiceClient(String clientKey) {
        return Optional.ofNullable(clients.get(clientKey));
    }

    @Override
    public MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount) {
        subAccounts.put(subAccount.subAccountId(), subAccount);
        return subAccount;
    }

    @Override
    public List<MerchantSubAccount> findSubAccounts(long merchantId) {
        return subAccounts.values().stream().filter(subAccount -> subAccount.merchantId() == merchantId).toList();
    }
}
