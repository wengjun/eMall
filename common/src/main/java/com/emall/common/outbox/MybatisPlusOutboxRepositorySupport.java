package com.emall.common.outbox;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.emall.common.persistence.BoundedQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

public abstract class MybatisPlusOutboxRepositorySupport implements OutboxRepository {
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    protected MybatisPlusOutboxRepositorySupport(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        if (event.status() == OutboxStatus.NEW && event.aggregateVersion() <= 0) {
            return insertAndAssignAggregateVersion(event);
        }
        OutboxEventRecord record = toRecord(event);
        try {
            outboxEventMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            if (event.status() == OutboxStatus.NEW) {
                OutboxEventRecord existing = outboxEventMapper.selectById(record.getEventId());
                if (existing != null) {
                    return toDomain(existing);
                }
                throw ex;
            }
            outboxEventMapper.update(null,
                    new UpdateWrapper<OutboxEventRecord>().set("status", record.getStatus())
                            .set("shard_id", record.getShardId()).set("retry_count", record.getRetryCount())
                            .set("next_retry_at", record.getNextRetryAt()).set("claimed_by", record.getClaimedBy())
                            .set("claimed_until", record.getClaimedUntil()).set("published_at", record.getPublishedAt())
                            .set("error_code", record.getErrorCode()).set("last_error", record.getLastError())
                            .set("updated_at", record.getUpdatedAt()).eq("event_id", record.getEventId()));
        }
        return event;
    }

    @Override
    public List<OutboxEvent> claimPublishable(String ownerId, Instant now, Duration leaseTtl, int limit) {
        LocalDateTime currentTime = databaseTime(now);
        List<OutboxEventRecord> candidates =
                outboxEventMapper.selectPublishableHeads(currentTime, BoundedQuery.limit(limit));
        List<OutboxEvent> claimed = new ArrayList<>();
        for (OutboxEventRecord candidate : candidates) {
            LocalDateTime claimDeadline = databaseTime(now.plus(leaseTtl));
            int updated = outboxEventMapper.update(null,
                    new UpdateWrapper<OutboxEventRecord>().set("status", OutboxStatus.PROCESSING.name())
                            .set("claimed_by", ownerId).set("claimed_until", claimDeadline)
                            .set("updated_at", currentTime).eq("event_id", candidate.getEventId())
                            .and(wrapper -> wrapper.in("status", OutboxStatus.NEW.name(), OutboxStatus.FAILED.name())
                                    .le("next_retry_at", currentTime).or().eq("status", OutboxStatus.PROCESSING.name())
                                    .le("claimed_until", currentTime)));
            if (updated == 1) {
                claimed.add(toDomain(outboxEventMapper.selectById(candidate.getEventId())));
            }
        }
        return claimed;
    }

    @Override
    public List<OutboxEvent> findPublishable(Instant now, int limit) {
        return outboxEventMapper.selectPublishableHeads(databaseTime(now), BoundedQuery.limit(limit)).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public int rescheduleFailed(Instant now, int limit) {
        LocalDateTime retryTime = databaseTime(now);
        return outboxEventMapper.update(null,
                new UpdateWrapper<OutboxEventRecord>().set("next_retry_at", retryTime).set("updated_at", retryTime)
                        .eq("status", OutboxStatus.FAILED.name()).orderByAsc("created_at")
                        .last("LIMIT " + BoundedQuery.limit(limit)));
    }

    private OutboxEventRecord toRecord(OutboxEvent event) {
        OutboxEventRecord record = new OutboxEventRecord();
        record.setEventId(event.eventId());
        record.setAggregateType(event.aggregateType());
        record.setAggregateId(event.aggregateId());
        record.setEventType(event.eventType());
        record.setSchemaVersion(event.schemaVersion());
        record.setAggregateVersion(event.aggregateVersion());
        record.setProducer(event.producer());
        record.setProducerVersion(event.producerVersion());
        record.setOccurredAt(databaseTime(event.occurredAt()));
        record.setTraceId(event.traceId());
        record.setCorrelationId(event.correlationId());
        record.setShardId(event.shardId());
        record.setPayload(serialize(event.payload()));
        record.setStatus(event.status().name());
        record.setRetryCount(event.retryCount());
        record.setNextRetryAt(databaseTime(event.nextRetryAt()));
        record.setClaimedBy(event.claimedBy());
        record.setClaimedUntil(databaseTime(event.claimedUntil()));
        record.setPublishedAt(databaseTime(event.publishedAt()));
        record.setErrorCode(event.errorCode());
        record.setLastError(event.lastError());
        record.setCreatedAt(databaseTime(event.createdAt()));
        record.setUpdatedAt(databaseTime(event.updatedAt()));
        return record;
    }

    private OutboxEvent toDomain(OutboxEventRecord record) {
        return new OutboxEvent(record.getEventId(), record.getAggregateType(), record.getAggregateId(),
                record.getEventType(), record.getSchemaVersion() == null ? 1 : record.getSchemaVersion(),
                record.getAggregateVersion() == null ? 0L : record.getAggregateVersion(), record.getProducer(),
                record.getProducerVersion(), domainTime(record.getOccurredAt()), record.getTraceId(),
                record.getCorrelationId(), deserialize(record.getPayload()), OutboxStatus.valueOf(record.getStatus()),
                record.getRetryCount(), domainTime(record.getNextRetryAt()), domainTime(record.getCreatedAt()),
                domainTime(record.getUpdatedAt()), record.getShardId() == null ? 0 : record.getShardId(),
                record.getClaimedBy(), domainTime(record.getClaimedUntil()), domainTime(record.getPublishedAt()),
                record.getErrorCode(), record.getLastError());
    }

    private OutboxEvent insertAndAssignAggregateVersion(OutboxEvent event) {
        OutboxEventRecord existing = outboxEventMapper.selectById(event.eventId());
        if (existing != null) {
            return toDomain(existing);
        }
        try {
            outboxEventMapper.insert(toRecord(event));
        } catch (DuplicateKeyException exception) {
            OutboxEventRecord concurrent = outboxEventMapper.selectById(event.eventId());
            if (concurrent != null) {
                return toDomain(concurrent);
            }
            throw exception;
        }
        String aggregateKey = event.aggregateType() + ':' + event.aggregateId();
        outboxEventMapper.advanceAggregateVersion(aggregateKey, databaseTime(event.createdAt()));
        Long version = outboxEventMapper.currentAggregateVersion(aggregateKey);
        if (version == null || version <= 0) {
            throw new DataAccessResourceFailureException("failed to allocate outbox aggregate version");
        }
        int updated = outboxEventMapper.update(null, new UpdateWrapper<OutboxEventRecord>()
                .set("aggregate_version", version).eq("event_id", event.eventId()).eq("aggregate_version", 0));
        if (updated != 1) {
            throw new DataAccessResourceFailureException("failed to persist outbox aggregate version");
        }
        return event.withAggregateVersion(version);
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
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
