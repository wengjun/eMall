package com.emall.eventplatform;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryEventPlatformRepository implements EventPlatformRepository {
    private final ConcurrentMap<Long, EventSchema> schemas = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, EventFieldClassification> classifications = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TrackingEvent> events = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PipelineOffset> offsets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MetricMaterialization> materializations = new ConcurrentHashMap<>();

    @Override
    public EventSchema saveSchema(EventSchema schema) {
        schemas.put(schema.schemaId(), schema);
        return schema;
    }

    @Override
    public Optional<EventSchema> findSchema(String eventName, int version) {
        return schemas.values().stream()
                .filter(schema -> schema.eventName().equals(eventName) && schema.version() == version).findFirst();
    }

    @Override
    public List<EventSchema> findSchemas() {
        return List.copyOf(schemas.values());
    }

    @Override
    public EventFieldClassification saveFieldClassification(EventFieldClassification classification) {
        classifications.put(classification.classificationId(), classification);
        return classification;
    }

    @Override
    public List<EventFieldClassification> findFieldClassifications() {
        return List.copyOf(classifications.values());
    }

    @Override
    public List<EventFieldClassification> findFieldClassifications(String eventName, int version) {
        return classifications.values().stream().filter(
                classification -> classification.eventName().equals(eventName) && classification.version() == version)
                .toList();
    }

    @Override
    public TrackingEvent saveEvent(TrackingEvent event) {
        events.put(event.eventId(), event);
        return event;
    }

    @Override
    public boolean eventExists(String eventKey) {
        return events.values().stream().anyMatch(event -> event.eventKey().equals(eventKey));
    }

    @Override
    public List<TrackingEvent> findEvents(String eventName) {
        return events.values().stream().filter(event -> event.eventName().equals(eventName)).toList();
    }

    @Override
    public List<TrackingEvent> findEvents() {
        return List.copyOf(events.values());
    }

    @Override
    public PipelineOffset saveOffset(PipelineOffset offset) {
        offsets.put(offset.offsetId(), offset);
        return offset;
    }

    @Override
    public MetricMaterialization saveMaterialization(MetricMaterialization materialization) {
        materializations.put(materialization.materializationId(), materialization);
        return materialization;
    }

    @Override
    public List<MetricMaterialization> findMaterializations() {
        return List.copyOf(materializations.values());
    }
}
