package com.emall.eventplatform;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcEventPlatformRepository implements EventPlatformRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcEventPlatformRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EventSchema saveSchema(EventSchema schema) {
        jdbcTemplate.update("""
                INSERT INTO event_schema
                    (schema_id, event_name, version, owner, json_schema, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE json_schema = VALUES(json_schema), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, schema.schemaId(), schema.eventName(), schema.version(), schema.owner(), schema.jsonSchema(),
                schema.status().name(), Timestamp.from(schema.createdAt()), Timestamp.from(schema.updatedAt()));
        return schema;
    }

    @Override
    public Optional<EventSchema> findSchema(String eventName, int version) {
        return jdbcTemplate.query("""
                SELECT * FROM event_schema
                WHERE event_name = ? AND version = ?
                """, this::mapSchema, eventName, version).stream().findFirst();
    }

    @Override
    public List<EventSchema> findSchemas() {
        return jdbcTemplate.query("SELECT * FROM event_schema", this::mapSchema);
    }

    @Override
    public TrackingEvent saveEvent(TrackingEvent event) {
        jdbcTemplate.update("""
                INSERT INTO tracking_event
                    (event_id, event_name, version, event_key, user_key, payload, late_event, occurred_at,
                    ingested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.eventName(), event.version(), event.eventKey(), event.userKey(),
                event.payload(), event.lateEvent(), Timestamp.from(event.occurredAt()),
                Timestamp.from(event.ingestedAt()));
        return event;
    }

    @Override
    public boolean eventExists(String eventKey) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tracking_event WHERE event_key = ?",
                Integer.class, eventKey);
        return count != null && count > 0;
    }

    @Override
    public List<TrackingEvent> findEvents(String eventName) {
        return jdbcTemplate.query("SELECT * FROM tracking_event WHERE event_name = ?", this::mapEvent, eventName);
    }

    @Override
    public List<TrackingEvent> findEvents() {
        return jdbcTemplate.query("SELECT * FROM tracking_event", this::mapEvent);
    }

    @Override
    public PipelineOffset saveOffset(PipelineOffset offset) {
        jdbcTemplate.update("""
                INSERT INTO pipeline_offset (offset_id, consumer_group, topic_name, processed_offset, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE processed_offset = VALUES(processed_offset), updated_at = VALUES(updated_at)
                """, offset.offsetId(), offset.consumerGroup(), offset.topicName(), offset.processedOffset(),
                Timestamp.from(offset.updatedAt()));
        return offset;
    }

    @Override
    public MetricMaterialization saveMaterialization(MetricMaterialization materialization) {
        jdbcTemplate.update("""
                INSERT INTO metric_materialization
                    (materialization_id, metric_name, window_key, event_count, late_event_count, materialized_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, materialization.materializationId(), materialization.metricName(), materialization.windowKey(),
                materialization.eventCount(), materialization.lateEventCount(),
                Timestamp.from(materialization.materializedAt()));
        return materialization;
    }

    @Override
    public List<MetricMaterialization> findMaterializations() {
        return jdbcTemplate.query("SELECT * FROM metric_materialization", this::mapMaterialization);
    }

    private EventSchema mapSchema(ResultSet rs, int rowNum) throws SQLException {
        return new EventSchema(rs.getLong("schema_id"), rs.getString("event_name"), rs.getInt("version"),
                rs.getString("owner"), rs.getString("json_schema"), SchemaStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private TrackingEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TrackingEvent(rs.getLong("event_id"), rs.getString("event_name"), rs.getInt("version"),
                rs.getString("event_key"), rs.getString("user_key"), rs.getString("payload"),
                rs.getBoolean("late_event"), rs.getTimestamp("occurred_at").toInstant(),
                rs.getTimestamp("ingested_at").toInstant());
    }

    private MetricMaterialization mapMaterialization(ResultSet rs, int rowNum) throws SQLException {
        return new MetricMaterialization(rs.getLong("materialization_id"), rs.getString("metric_name"),
                rs.getString("window_key"), rs.getLong("event_count"), rs.getLong("late_event_count"),
                rs.getTimestamp("materialized_at").toInstant());
    }
}
