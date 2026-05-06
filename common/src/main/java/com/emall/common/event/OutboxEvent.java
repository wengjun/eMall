package com.emall.common.event;

import java.time.Instant;
import java.util.Map;

public record OutboxEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        Map<String, Object> payload,
        OutboxStatus status,
        int retryCount,
        Instant nextRetryAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static OutboxEvent create(String eventId, String aggregateType, String aggregateId,
                                     String eventType, Map<String, Object> payload) {
        Instant now = Instant.now();
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload,
                OutboxStatus.NEW, 0, now, now, now);
    }

    public OutboxEvent published() {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload,
                OutboxStatus.PUBLISHED, retryCount, nextRetryAt, createdAt, Instant.now());
    }

    public OutboxEvent failed(Instant nextRetryAt) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload,
                OutboxStatus.FAILED, retryCount + 1, nextRetryAt, createdAt, Instant.now());
    }

    public OutboxEvent readyForRetry(Instant now) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload,
                OutboxStatus.FAILED, retryCount, now, createdAt, now);
    }
}
