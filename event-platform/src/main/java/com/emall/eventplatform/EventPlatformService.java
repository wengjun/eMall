package com.emall.eventplatform;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class EventPlatformService {
    private final EventPlatformRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    EventPlatformService(EventPlatformRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    EventSchema registerSchema(String eventName, int version, String owner, String jsonSchema) {
        if (repository.findSchema(normalize(eventName), version).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "event schema already exists");
        }
        Instant now = Instant.now();
        return repository.saveSchema(new EventSchema(idGenerator.nextId(), normalize(eventName), version,
                normalize(owner), jsonSchema, SchemaStatus.DRAFT, now, now));
    }

    @Transactional
    EventSchema activateSchema(String eventName, int version) {
        EventSchema schema = repository.findSchema(normalize(eventName), version)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "event schema not found"));
        return repository.saveSchema(schema.activate());
    }

    @Transactional
    EventFieldClassification classifyField(String eventName, int version, String fieldName,
            SensitiveDataType sensitivity, boolean required, boolean exportedToWarehouse) {
        requireSchema(eventName, version);
        return repository
                .saveFieldClassification(new EventFieldClassification(idGenerator.nextId(), normalize(eventName),
                        version, normalize(fieldName), sensitivity, required, exportedToWarehouse, Instant.now()));
    }

    @Transactional
    TrackingEvent ingestEvent(String eventName, int version, String eventKey, String userKey, String payload,
            Instant occurredAt) {
        EventSchema schema = requireSchema(eventName, version);
        if (schema.status() != SchemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "event schema is not active");
        }
        if (repository.eventExists(normalize(eventKey))) {
            throw new BusinessException(ErrorCode.CONFLICT, "tracking event already ingested");
        }
        Instant now = Instant.now();
        boolean lateEvent = occurredAt.isBefore(now.minus(Duration.ofHours(24)));
        return repository
                .saveEvent(new TrackingEvent(idGenerator.nextId(), schema.eventName(), version, normalize(eventKey),
                        normalize(userKey), SensitiveDataMasker.maskFreeText(payload), lateEvent, occurredAt, now));
    }

    @Transactional
    PipelineOffset commitOffset(String consumerGroup, String topicName, long processedOffset) {
        return repository.saveOffset(new PipelineOffset(idGenerator.nextId(), normalize(consumerGroup),
                normalize(topicName), processedOffset, Instant.now()));
    }

    @Transactional
    MetricMaterialization materializeMetric(String eventName, String metricName, String windowKey) {
        String normalizedEventName = normalize(eventName);
        long eventCount = repository.countEvents(normalizedEventName, null);
        long lateEventCount = repository.countEvents(normalizedEventName, true);
        return repository.saveMaterialization(new MetricMaterialization(idGenerator.nextId(), normalize(metricName),
                normalize(windowKey), eventCount, lateEventCount, Instant.now()));
    }

    EventPlatformSummary summary() {
        return new EventPlatformSummary(repository.countSchemas(SchemaStatus.ACTIVE),
                repository.countEvents(null, null), repository.countEvents(null, true),
                repository.countMaterializations(), repository.countFieldClassifications());
    }

    private EventSchema requireSchema(String eventName, int version) {
        return repository.findSchema(normalize(eventName), version)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "event schema not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "event platform value must not be blank");
        }
        return normalized;
    }
}
