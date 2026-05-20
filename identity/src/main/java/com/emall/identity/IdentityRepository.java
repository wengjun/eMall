package com.emall.identity;

import java.util.List;
import java.util.Optional;

interface IdentityRepository {
    IdentityAccount saveAccount(IdentityAccount account);

    Optional<IdentityAccount> findAccount(long accountId);

    Optional<IdentityAccount> findAccountBySubject(String subject);

    DeviceSession saveSession(DeviceSession session);

    Optional<DeviceSession> findSession(long sessionId);

    Optional<DeviceSession> findSessionByAccessToken(String accessToken);

    PermissionGrant saveGrant(PermissionGrant grant);

    List<PermissionGrant> findGrants(long accountId);

    ServiceClient saveServiceClient(ServiceClient client);

    Optional<ServiceClient> findServiceClient(String clientKey);

    MerchantSubAccount saveSubAccount(MerchantSubAccount subAccount);

    List<MerchantSubAccount> findSubAccounts(long merchantId);
}
