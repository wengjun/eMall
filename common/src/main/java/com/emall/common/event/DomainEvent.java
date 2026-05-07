package com.emall.common.event;

import java.time.Instant;
import java.util.Map;

public record DomainEvent(String eventId, String aggregateType, String aggregateId, String eventType,
        Map<String, Object> payload, Instant occurredAt) {
    public static DomainEvent of(String eventId, String aggregateType, String aggregateId, String eventType,
            Map<String, Object> payload) {
        return new DomainEvent(eventId, aggregateType, aggregateId, eventType, payload, Instant.now());
    }
}
