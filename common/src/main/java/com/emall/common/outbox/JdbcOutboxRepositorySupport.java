package com.emall.common.outbox;

import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class JdbcOutboxRepositorySupport implements OutboxRepository {
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    protected JdbcOutboxRepositorySupport(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        jdbcTemplate.update("""
                INSERT INTO outbox_event
                    (event_id, aggregate_type, aggregate_id, event_type, payload, status, retry_count,
                    next_retry_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), retry_count = VALUES(retry_count),
                    next_retry_at = VALUES(next_retry_at), updated_at = VALUES(updated_at)
                """,
                event.eventId(), event.aggregateType(), event.aggregateId(), event.eventType(),
                serialize(event.payload()), event.status().name(), event.retryCount(),
                Timestamp.from(event.nextRetryAt()), Timestamp.from(event.createdAt()),
                Timestamp.from(event.updatedAt()));
        return event;
    }

    @Override
    public List<OutboxEvent> findPublishable(Instant now, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM outbox_event
                WHERE status IN ('NEW', 'FAILED') AND next_retry_at <= ?
                ORDER BY created_at ASC
                LIMIT ?
                """, this::map, Timestamp.from(now), limit);
    }

    @Override
    public int rescheduleFailed(Instant now, int limit) {
        return jdbcTemplate.update("""
                UPDATE outbox_event
                SET next_retry_at = ?, updated_at = ?
                WHERE status = 'FAILED'
                ORDER BY created_at ASC
                LIMIT ?
                """, Timestamp.from(now), Timestamp.from(now), limit);
    }

    private OutboxEvent map(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxEvent(
                rs.getString("event_id"),
                rs.getString("aggregate_type"),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                deserialize(rs.getString("payload")),
                OutboxStatus.valueOf(rs.getString("status")),
                rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new DataAccessResourceFailureException("failed to serialize outbox payload", ex);
        }
    }

    private Map<String, Object> deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, PAYLOAD_TYPE);
        } catch (JsonProcessingException ex) {
            throw new DataAccessResourceFailureException("failed to deserialize outbox payload", ex);
        }
    }
}
