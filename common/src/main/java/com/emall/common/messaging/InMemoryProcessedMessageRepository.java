package com.emall.common.messaging;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class InMemoryProcessedMessageRepository implements ProcessedMessageRepository {
    private static final Duration PROCESSING_LEASE_TTL = Duration.ofMinutes(5);

    private final ConcurrentMap<String, ProcessedMessage> messages = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryProcessedMessageRepository() {
        this(Clock.systemUTC());
    }

    public InMemoryProcessedMessageRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean markProcessing(String messageId) {
        Instant now = clock.instant();
        AtomicBoolean claimed = new AtomicBoolean();
        messages.compute(messageId, (ignored, existing) -> {
            if (existing == null || existing.status() == ProcessedMessageStatus.FAILED
                    || processingLeaseExpired(existing, now)) {
                claimed.set(true);
                int retryCount = existing == null ? 0 : existing.retryCount();
                return new ProcessedMessage(messageId, ProcessedMessageStatus.PROCESSING, retryCount, null, null, now,
                        null);
            }
            return existing;
        });
        return claimed.get();
    }

    @Override
    public void markProcessed(String messageId) {
        Instant now = clock.instant();
        messages.compute(messageId, (ignored, existing) -> new ProcessedMessage(messageId,
                ProcessedMessageStatus.PROCESSED, existing == null ? 0 : existing.retryCount(), null, null, now, null));
    }

    @Override
    public int markFailed(String messageId, String errorCode, String lastError) {
        Instant now = clock.instant();
        ProcessedMessage result = messages.compute(messageId, (ignored, existing) -> {
            int retryCount = existing == null ? 1 : existing.retryCount() + 1;
            return new ProcessedMessage(messageId, ProcessedMessageStatus.FAILED, retryCount, errorCode, lastError, now,
                    null);
        });
        return result.retryCount();
    }

    @Override
    public void markDead(String messageId, String errorCode, String lastError) {
        Instant now = clock.instant();
        messages.compute(messageId, (ignored, existing) -> new ProcessedMessage(messageId, ProcessedMessageStatus.DEAD,
                existing == null ? 0 : existing.retryCount(), errorCode, lastError, now, now));
    }

    private boolean processingLeaseExpired(ProcessedMessage message, Instant now) {
        return message.status() == ProcessedMessageStatus.PROCESSING
                && message.updatedAt().plus(PROCESSING_LEASE_TTL).isBefore(now);
    }
}
