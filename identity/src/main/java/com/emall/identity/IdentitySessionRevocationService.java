package com.emall.identity;

import com.emall.common.security.AuthSecurityProperties;
import com.emall.common.security.TokenRevocationStore;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class IdentitySessionRevocationService {
    private static final int REVOCATION_BATCH_SIZE = 500;

    private final IdentityRepository repository;
    private final TokenRevocationStore revocationStore;
    private final AuthSecurityProperties securityProperties;

    IdentitySessionRevocationService(IdentityRepository repository, TokenRevocationStore revocationStore,
            AuthSecurityProperties securityProperties) {
        this.repository = repository;
        this.revocationStore = revocationStore;
        this.securityProperties = securityProperties;
    }

    void revokeAll(long accountId, Instant now) {
        long cursor = 0L;
        Instant accessTokenCutoff = now.minus(securityProperties.getAccessTokenTtl());
        while (true) {
            List<DeviceSession> sessions = repository.findActiveSessionsForRevocation(accountId, cursor,
                    accessTokenCutoff, REVOCATION_BATCH_SIZE);
            for (DeviceSession session : sessions) {
                revocationStore.revoke(session.sessionId(), securityProperties.getAccessTokenTtl());
                cursor = session.sessionId();
            }
            if (sessions.size() < REVOCATION_BATCH_SIZE) {
                break;
            }
        }
        repository.revokeAllSessions(accountId, now);
    }
}
