package com.emall.common.outbox;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

public abstract class MybatisPlusOutboxRepositorySupport implements OutboxRepository {
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final BaseMapper<OutboxEventRecord> outboxEventMapper;
    private final ObjectMapper objectMapper;

    protected MybatisPlusOutboxRepositorySupport(
            BaseMapper<OutboxEventRecord> outboxEventMapper, ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventRecord record = toRecord(event);
        try {
            outboxEventMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            outboxEventMapper.update(null, new UpdateWrapper<OutboxEventRecord>()
                    .set("status", record.getStatus())
                    .set("retry_count", record.getRetryCount())
                    .set("next_retry_at", record.getNextRetryAt())
                    .set("updated_at", record.getUpdatedAt())
                    .eq("event_id", record.getEventId()));
        }
        return event;
    }

    @Override
    public List<OutboxEvent> findPublishable(Instant now, int limit) {
        return outboxEventMapper.selectList(new QueryWrapper<OutboxEventRecord>()
                .in("status", OutboxStatus.NEW.name(), OutboxStatus.FAILED.name())
                .le("next_retry_at", databaseTime(now))
                .orderByAsc("created_at")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    @Override
    public int rescheduleFailed(Instant now, int limit) {
        LocalDateTime retryTime = databaseTime(now);
        return outboxEventMapper.update(null, new UpdateWrapper<OutboxEventRecord>()
                .set("next_retry_at", retryTime)
                .set("updated_at", retryTime)
                .eq("status", OutboxStatus.FAILED.name())
                .orderByAsc("created_at")
                .last("LIMIT " + limit));
    }

    private OutboxEventRecord toRecord(OutboxEvent event) {
        OutboxEventRecord record = new OutboxEventRecord();
        record.setEventId(event.eventId());
        record.setAggregateType(event.aggregateType());
        record.setAggregateId(event.aggregateId());
        record.setEventType(event.eventType());
        record.setPayload(serialize(event.payload()));
        record.setStatus(event.status().name());
        record.setRetryCount(event.retryCount());
        record.setNextRetryAt(databaseTime(event.nextRetryAt()));
        record.setCreatedAt(databaseTime(event.createdAt()));
        record.setUpdatedAt(databaseTime(event.updatedAt()));
        return record;
    }

    private OutboxEvent toDomain(OutboxEventRecord record) {
        return new OutboxEvent(record.getEventId(), record.getAggregateType(), record.getAggregateId(),
                record.getEventType(), deserialize(record.getPayload()), OutboxStatus.valueOf(record.getStatus()),
                record.getRetryCount(), domainTime(record.getNextRetryAt()), domainTime(record.getCreatedAt()),
                domainTime(record.getUpdatedAt()));
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

    private LocalDateTime databaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC);
    }
}
