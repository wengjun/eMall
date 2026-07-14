package com.emall.common.security;

import java.time.Duration;

public interface TokenRevocationStore {
    boolean isRevoked(long sessionId);

    void revoke(long sessionId, Duration ttl);
}
