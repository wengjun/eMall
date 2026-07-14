package com.emall.eventplatform;

import java.util.List;
import java.util.Optional;

interface EventPlatformRepository {
    EventSchema saveSchema(EventSchema schema);

    Optional<EventSchema> findSchema(String eventName, int version);

    List<EventSchema> findSchemas();

    long countSchemas(SchemaStatus status);

    EventFieldClassification saveFieldClassification(EventFieldClassification classification);

    List<EventFieldClassification> findFieldClassifications();

    long countFieldClassifications();

    List<EventFieldClassification> findFieldClassifications(String eventName, int version);

    TrackingEvent saveEvent(TrackingEvent event);

    boolean eventExists(String eventKey);

    List<TrackingEvent> findEvents(String eventName);

    List<TrackingEvent> findEvents();

    long countEvents(String eventName, Boolean lateEvent);

    PipelineOffset saveOffset(PipelineOffset offset);

    MetricMaterialization saveMaterialization(MetricMaterialization materialization);

    List<MetricMaterialization> findMaterializations();

    long countMaterializations();
}
