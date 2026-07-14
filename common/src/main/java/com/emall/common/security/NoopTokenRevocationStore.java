package com.emall.common.security;

import java.time.Duration;

final class NoopTokenRevocationStore implements TokenRevocationStore {
    @Override
    public boolean isRevoked(long sessionId) {
        return false;
    }

    @Override
    public void revoke(long sessionId, Duration ttl) {
        // Revocation is enforced by the identity session store when Redis is not
        // configured.
    }
}
