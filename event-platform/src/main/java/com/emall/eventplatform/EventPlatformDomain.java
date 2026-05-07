package com.emall.eventplatform;

import java.time.Instant;

enum SchemaStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED
}

record EventSchema(long schemaId, String eventName, int version, String owner, String jsonSchema, SchemaStatus status,
        Instant createdAt, Instant updatedAt) {
    EventSchema activate() {
        return new EventSchema(schemaId, eventName, version, owner, jsonSchema, SchemaStatus.ACTIVE, createdAt,
                Instant.now());
    }
}

record TrackingEvent(long eventId, String eventName, int version, String eventKey, String userKey, String payload,
        boolean lateEvent, Instant occurredAt, Instant ingestedAt) {
}

record PipelineOffset(long offsetId, String consumerGroup, String topicName, long processedOffset, Instant updatedAt) {
}

record MetricMaterialization(long materializationId, String metricName, String windowKey, long eventCount,
        long lateEventCount, Instant materializedAt) {
}

record EventPlatformSummary(int activeSchemas, int ingestedEvents, int lateEvents, int materializedMetrics) {
}
