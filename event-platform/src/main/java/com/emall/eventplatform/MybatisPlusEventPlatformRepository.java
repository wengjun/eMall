package com.emall.eventplatform;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(eventPlatformMapper.findSchema(eventName, version)).map(this::mapSchema);
    }

    @Override
    public List<EventSchema> findSchemas() {
        return eventPlatformMapper.findSchemas().stream().map(this::mapSchema).toList();
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
        return eventPlatformMapper.findEventsByName(eventName).stream().map(this::mapEvent).toList();
    }

    @Override
    public List<TrackingEvent> findEvents() {
        return eventPlatformMapper.findEvents().stream().map(this::mapEvent).toList();
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
        return eventPlatformMapper.findMaterializations().stream().map(this::mapMaterialization).toList();
    }

    private EventSchema mapSchema(Map<String, Object> row) {
        return new EventSchema(longValue(row, "schema_id"), stringValue(row, "event_name"),
                intValue(row, "version"), stringValue(row, "owner"), stringValue(row, "json_schema"),
                SchemaStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private TrackingEvent mapEvent(Map<String, Object> row) {
        return new TrackingEvent(longValue(row, "event_id"), stringValue(row, "event_name"),
                intValue(row, "version"), stringValue(row, "event_key"), stringValue(row, "user_key"),
                stringValue(row, "payload"), booleanValue(row, "late_event"), instantValue(row, "occurred_at"),
                instantValue(row, "ingested_at"));
    }

    private MetricMaterialization mapMaterialization(Map<String, Object> row) {
        return new MetricMaterialization(longValue(row, "materialization_id"), stringValue(row, "metric_name"),
                stringValue(row, "window_key"), longValue(row, "event_count"), longValue(row, "late_event_count"),
                instantValue(row, "materialized_at"));
    }
}
