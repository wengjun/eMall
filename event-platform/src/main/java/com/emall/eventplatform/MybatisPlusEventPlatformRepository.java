package com.emall.eventplatform;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusEventPlatformRepository implements EventPlatformRepository {
    private final EventPlatformMapper eventPlatformMapper;

    MybatisPlusEventPlatformRepository(EventPlatformMapper eventPlatformMapper) {
        this.eventPlatformMapper = eventPlatformMapper;
    }

    @Override
    public EventSchema saveSchema(EventSchema schema) {
        eventPlatformMapper.saveSchema(schema);
        return schema;
    }

    @Override
    public Optional<EventSchema> findSchema(String eventName, int version) {
        return Optional.ofNullable(eventPlatformMapper.findSchema(eventName, version));
    }

    @Override
    public List<EventSchema> findSchemas() {
        return eventPlatformMapper.findSchemas();
    }

    @Override
    public TrackingEvent saveEvent(TrackingEvent event) {
        eventPlatformMapper.saveEvent(event);
        return event;
    }

    @Override
    public boolean eventExists(String eventKey) {
        return eventPlatformMapper.countEventsByKey(eventKey) > 0;
    }

    @Override
    public List<TrackingEvent> findEvents(String eventName) {
        return eventPlatformMapper.findEventsByName(eventName);
    }

    @Override
    public List<TrackingEvent> findEvents() {
        return eventPlatformMapper.findEvents();
    }

    @Override
    public PipelineOffset saveOffset(PipelineOffset offset) {
        eventPlatformMapper.saveOffset(offset);
        return offset;
    }

    @Override
    public MetricMaterialization saveMaterialization(MetricMaterialization materialization) {
        eventPlatformMapper.saveMaterialization(materialization);
        return materialization;
    }

    @Override
    public List<MetricMaterialization> findMaterializations() {
        return eventPlatformMapper.findMaterializations();
    }
}
