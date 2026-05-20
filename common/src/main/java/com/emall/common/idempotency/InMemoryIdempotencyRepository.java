package com.emall.common.idempotency;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotencyRepository implements IdempotencyRepository {
    private final ConcurrentMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return Optional.ofNullable(records.get(key));
    }

    @Override
    public boolean insertProcessing(IdempotencyRecord record) {
        return records.putIfAbsent(record.key(), record) == null;
    }

    @Override
    public boolean replace(String key, IdempotencyStatus expectedStatus, IdempotencyRecord record) {
        AtomicFlag updated = new AtomicFlag();
        records.computeIfPresent(key, (currentKey, existing) -> {
            if (existing.status() != expectedStatus) {
                return existing;
            }
            updated.set();
            return record;
        });
        return updated.value();
    }

    @Override
    public int deleteExpired(Instant now, int limit) {
        List<String> expiredKeys = records.values().stream()
                .filter(record -> record.expiresAt() != null && !record.expiresAt().isAfter(now))
                .sorted(Comparator.comparing(IdempotencyRecord::expiresAt)).limit(limit).map(IdempotencyRecord::key)
                .toList();
        expiredKeys.forEach(records::remove);
        return expiredKeys.size();
    }

    private static final class AtomicFlag {
        private boolean value;

        void set() {
            value = true;
        }

        boolean value() {
            return value;
        }
    }
}
