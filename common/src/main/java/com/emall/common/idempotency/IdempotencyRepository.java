package com.emall.common.idempotency;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRepository {
    Optional<IdempotencyRecord> find(String key);

    boolean insertProcessing(IdempotencyRecord record);

    boolean replace(String key, IdempotencyStatus expectedStatus, IdempotencyRecord record);

    int deleteExpired(Instant now, int limit);
}
