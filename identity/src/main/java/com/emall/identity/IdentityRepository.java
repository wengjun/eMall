package com.emall.identity;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

interface IdentityRepository {
    IdentityAccount saveAccount(IdentityAccount account);

    Optional<IdentityAccount> findAccount(long accountId);

    Optional<IdentityAccount> findAccountBySubject(String subject);

    IdentityCredential saveCredential(IdentityCredential credential);

    Optional<IdentityCredential> findCredential(long accountId);

    void recordCredentialFailure(long accountId, Instant now, Instant lockedUntil, int maximumAttempts);

    void clearCredentialFailures(long accountId, Instant now);

    DeviceSession saveSession(DeviceSession session);

    Optional<DeviceSession> findSession(long sessionId);

    Optional<DeviceSession> findSessionByAccessToken(String accessToken);

    Optional<DeviceSession> findSessionByRefreshToken(String refreshToken);

    boolean revokeSessionIfActive(long sessionId, String refreshToken, Instant updatedAt);

    PermissionGrant saveGrant(PermissionGrant grant);

    List<PermissionGrant> findGrants(long accountId);

    ServiceClient saveServiceClient(ServiceClient client);

    Optional<ServiceClient> findServiceClient(String clientKey);

    MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount);

    List<MerchantSubAccount> findSubAccounts(long merchantId);
}
