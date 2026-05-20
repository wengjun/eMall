package com.emall.search.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryProcessedMessageRepository implements ProcessedMessageRepository {
    private static final java.time.Duration PROCESSING_LEASE_TTL = java.time.Duration.ofMinutes(5);
    private final ConcurrentMap<String, MessageState> messages = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryProcessedMessageRepository() {
        this(Clock.systemUTC());
    }

    public InMemoryProcessedMessageRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public boolean markProcessing(String messageId) {
        AtomicBoolean claimed = new AtomicBoolean();
        Instant now = Instant.now(clock);
        messages.compute(messageId, (key, existing) -> {
            if (existing == null || existing.status() == ProcessedMessageStatus.FAILED
                    || existing.processingExpired(now)) {
                claimed.set(true);
                int retryCount = existing == null ? 0 : existing.retryCount();
                return new MessageState(ProcessedMessageStatus.PROCESSING, retryCount, null, null, now);
            }
            return existing;
        });
        return claimed.get();
    }

    @Override
    public void markProcessed(String messageId) {
        messages.computeIfPresent(messageId, (key, existing) -> existing.processed(Instant.now(clock)));
    }

    @Override
    public int markFailed(String messageId, String errorCode, String lastError) {
        Instant now = Instant.now(clock);
        MessageState state = messages.compute(messageId, (key, existing) -> {
            MessageState current = existing == null
                    ? new MessageState(ProcessedMessageStatus.PROCESSING, 0, null, null, now)
                    : existing;
            return current.failed(errorCode, lastError, now);
        });
        return state.retryCount();
    }

    @Override
    public void markDead(String messageId, String errorCode, String lastError) {
        Instant now = Instant.now(clock);
        messages.compute(messageId, (key, existing) -> {
            MessageState current =
                    existing == null ? new MessageState(ProcessedMessageStatus.FAILED, 0, null, null, now) : existing;
            return current.dead(errorCode, lastError, now);
        });
    }

    private record MessageState(ProcessedMessageStatus status, int retryCount, String errorCode, String lastError,
            Instant updatedAt) {
        MessageState processed(Instant now) {
            return new MessageState(ProcessedMessageStatus.PROCESSED, retryCount, null, null, now);
        }

        MessageState failed(String nextErrorCode, String nextLastError, Instant now) {
            return new MessageState(ProcessedMessageStatus.FAILED, retryCount + 1, nextErrorCode, nextLastError, now);
        }

        MessageState dead(String nextErrorCode, String nextLastError, Instant now) {
            return new MessageState(ProcessedMessageStatus.DEAD, retryCount, nextErrorCode, nextLastError, now);
        }

        boolean processingExpired(Instant now) {
            return status == ProcessedMessageStatus.PROCESSING && updatedAt.plus(PROCESSING_LEASE_TTL).isBefore(now);
        }
    }
}
