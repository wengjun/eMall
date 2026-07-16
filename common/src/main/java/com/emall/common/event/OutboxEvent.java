package com.emall.common.event;

import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;

public record OutboxEvent(String eventId, String aggregateType, String aggregateId, String eventType, int schemaVersion,
        long aggregateVersion, String producer, String producerVersion, Instant occurredAt, String traceId,
        String correlationId, Map<String, Object> payload, OutboxStatus status, int retryCount, Instant nextRetryAt,
        Instant createdAt, Instant updatedAt, int shardId, String claimedBy, Instant claimedUntil, Instant publishedAt,
        String errorCode, String lastError) {
    private static final String LEGACY_PRODUCER = "legacy";

    public OutboxEvent {
        schemaVersion = schemaVersion <= 0 ? 1 : schemaVersion;
        producer = textOrDefault(producer, LEGACY_PRODUCER);
        producerVersion = textOrDefault(producerVersion, "unknown");
        occurredAt = occurredAt == null ? createdAt : occurredAt;
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static OutboxEvent create(String eventId, String aggregateType, String aggregateId, String eventType,
            String producer, String producerVersion, VersionedEventPayload payload) {
        Instant now = Instant.now();
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload.schemaVersion(), 0L, producer,
                producerVersion, now, MDC.get("traceId"), MDC.get("correlationId"), payload.toMap(), OutboxStatus.NEW,
                0, now, now, now, shardId(aggregateId), null, null, null, null, null);
    }

    @Deprecated(forRemoval = false)
    public static OutboxEvent create(String eventId, String aggregateType, String aggregateId, String eventType,
            Map<String, Object> payload) {
        Instant now = Instant.now();
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, 1, 0L, LEGACY_PRODUCER, "unknown", now,
                MDC.get("traceId"), MDC.get("correlationId"), payload, OutboxStatus.NEW, 0, now, now, now,
                shardId(aggregateId), null, null, null, null, null);
    }

    public OutboxEvent withAggregateVersion(long version) {
        if (version <= 0) {
            throw new IllegalArgumentException("aggregate version must be positive");
        }
        return copy(status, retryCount, nextRetryAt, updatedAt, claimedBy, claimedUntil, publishedAt, errorCode,
                lastError, version);
    }

    public boolean legacyContract() {
        return LEGACY_PRODUCER.equals(producer);
    }

    public OutboxEvent claimed(String ownerId, Instant leaseDeadline) {
        return copy(OutboxStatus.PROCESSING, retryCount, nextRetryAt, Instant.now(), ownerId, leaseDeadline,
                publishedAt, errorCode, lastError, aggregateVersion);
    }

    public OutboxEvent published() {
        Instant now = Instant.now();
        return copy(OutboxStatus.PUBLISHED, retryCount, nextRetryAt, now, null, null, now, null, null,
                aggregateVersion);
    }

    public OutboxEvent failed(Instant retryAt) {
        return failed(retryAt, "PUBLISH_FAILED", null);
    }

    public OutboxEvent failed(Instant retryAt, String nextErrorCode, String nextLastError) {
        return copy(OutboxStatus.FAILED, retryCount + 1, retryAt, Instant.now(), null, null, publishedAt, nextErrorCode,
                truncate(nextLastError), aggregateVersion);
    }

    public OutboxEvent dead(String nextErrorCode, String nextLastError) {
        return copy(OutboxStatus.DEAD, retryCount + 1, nextRetryAt, Instant.now(), null, null, publishedAt,
                nextErrorCode, truncate(nextLastError), aggregateVersion);
    }

    public OutboxEvent readyForRetry(Instant now) {
        return copy(OutboxStatus.FAILED, retryCount, now, now, null, null, publishedAt, errorCode, lastError,
                aggregateVersion);
    }

    private OutboxEvent copy(OutboxStatus nextStatus, int nextRetryCount, Instant retryAt, Instant nextUpdatedAt,
            String nextClaimedBy, Instant nextClaimedUntil, Instant nextPublishedAt, String nextErrorCode,
            String nextLastError, long nextAggregateVersion) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, schemaVersion, nextAggregateVersion,
                producer, producerVersion, occurredAt, traceId, correlationId, payload, nextStatus, nextRetryCount,
                retryAt, createdAt, nextUpdatedAt, shardId, nextClaimedBy, nextClaimedUntil, nextPublishedAt,
                nextErrorCode, nextLastError);
    }

    private static int shardId(String aggregateId) {
        return Math.floorMod(aggregateId.hashCode(), 1024);
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 512) {
            return value;
        }
        return value.substring(0, 512);
    }
}
