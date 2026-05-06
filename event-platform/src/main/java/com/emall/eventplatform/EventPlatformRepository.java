package com.emall.eventplatform;

import java.util.List;
import java.util.Optional;

interface EventPlatformRepository {
    EventSchema saveSchema(EventSchema schema);

    Optional<EventSchema> findSchema(String eventName, int version);

    List<EventSchema> findSchemas();

    TrackingEvent saveEvent(TrackingEvent event);

    boolean eventExists(String eventKey);

    List<TrackingEvent> findEvents(String eventName);

    List<TrackingEvent> findEvents();

    PipelineOffset saveOffset(PipelineOffset offset);

    MetricMaterialization saveMaterialization(MetricMaterialization materialization);

    List<MetricMaterialization> findMaterializations();
}
