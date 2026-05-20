package com.emall.common.event;

import java.time.Instant;
import java.util.Map;

public record OutboxEvent(String eventId, String aggregateType, String aggregateId, String eventType,
        Map<String, Object> payload, OutboxStatus status, int retryCount, Instant nextRetryAt, Instant createdAt,
        Instant updatedAt, int shardId, String claimedBy, Instant claimedUntil, Instant publishedAt, String errorCode,
        String lastError) {
    public static OutboxEvent create(String eventId, String aggregateType, String aggregateId, String eventType,
            Map<String, Object> payload) {
        Instant now = Instant.now();
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, OutboxStatus.NEW, 0, now, now,
                now, shardId(aggregateId), null, null, null, null, null);
    }

    public OutboxEvent claimed(String ownerId, Instant claimedUntil) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, OutboxStatus.PROCESSING,
                retryCount, nextRetryAt, createdAt, Instant.now(), shardId, ownerId, claimedUntil, publishedAt,
                errorCode, lastError);
    }

    public OutboxEvent published() {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, OutboxStatus.PUBLISHED,
                retryCount, nextRetryAt, createdAt, Instant.now(), shardId, null, null, Instant.now(), null, null);
    }

    public OutboxEvent failed(Instant nextRetryAt) {
        return failed(nextRetryAt, "PUBLISH_FAILED", null);
    }

    public OutboxEvent failed(Instant nextRetryAt, String errorCode, String lastError) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, OutboxStatus.FAILED,
                retryCount + 1, nextRetryAt, createdAt, Instant.now(), shardId, null, null, publishedAt, errorCode,
                truncate(lastError));
    }

    public OutboxEvent dead(String errorCode, String lastError) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, OutboxStatus.DEAD,
                retryCount + 1, nextRetryAt, createdAt, Instant.now(), shardId, null, null, publishedAt, errorCode,
                truncate(lastError));
    }

    public OutboxEvent readyForRetry(Instant now) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, OutboxStatus.FAILED, retryCount,
                now, createdAt, now, shardId, null, null, publishedAt, errorCode, lastError);
    }

    private static int shardId(String aggregateId) {
        return Math.floorMod(aggregateId.hashCode(), 1024);
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 512) {
            return value;
        }
        return value.substring(0, 512);
    }
}
