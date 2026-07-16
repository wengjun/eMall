package com.emall.common.event;

import java.time.Instant;
import java.util.Map;

public record DomainEvent(String eventId, String aggregateType, String aggregateId, String eventType, int schemaVersion,
        long aggregateVersion, String producer, String producerVersion, Map<String, Object> payload,
        Instant occurredAt) {
    public static DomainEvent create(String eventId, String aggregateType, String aggregateId, String eventType,
            long aggregateVersion, String producer, String producerVersion, VersionedEventPayload payload) {
        return new DomainEvent(eventId, aggregateType, aggregateId, eventType, payload.schemaVersion(),
                aggregateVersion, producer, producerVersion, payload.toMap(), Instant.now());
    }
}
