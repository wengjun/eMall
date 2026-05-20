package com.emall.common.outbox;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
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

public abstract class MybatisPlusOutboxRepositorySupport implements OutboxRepository {
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final BaseMapper<OutboxEventRecord> outboxEventMapper;
    private final ObjectMapper objectMapper;

    protected MybatisPlusOutboxRepositorySupport(BaseMapper<OutboxEventRecord> outboxEventMapper,
            ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventRecord record = toRecord(event);
        try {
            outboxEventMapper.insert(record);
        } catch (DuplicateKeyException ex) {
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
        List<OutboxEventRecord> candidates = outboxEventMapper.selectList(publishableQuery(currentTime, limit));
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
        return outboxEventMapper.selectList(publishableQuery(databaseTime(now), limit)).stream().map(this::toDomain)
                .toList();
    }

    @Override
    public int rescheduleFailed(Instant now, int limit) {
        LocalDateTime retryTime = databaseTime(now);
        return outboxEventMapper.update(null,
                new UpdateWrapper<OutboxEventRecord>().set("next_retry_at", retryTime).set("updated_at", retryTime)
                        .eq("status", OutboxStatus.FAILED.name()).orderByAsc("created_at").last("LIMIT " + limit));
    }

    private OutboxEventRecord toRecord(OutboxEvent event) {
        OutboxEventRecord record = new OutboxEventRecord();
        record.setEventId(event.eventId());
        record.setAggregateType(event.aggregateType());
        record.setAggregateId(event.aggregateId());
        record.setEventType(event.eventType());
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
                record.getEventType(), deserialize(record.getPayload()), OutboxStatus.valueOf(record.getStatus()),
                record.getRetryCount(), domainTime(record.getNextRetryAt()), domainTime(record.getCreatedAt()),
                domainTime(record.getUpdatedAt()), record.getShardId() == null ? 0 : record.getShardId(),
                record.getClaimedBy(), domainTime(record.getClaimedUntil()), domainTime(record.getPublishedAt()),
                record.getErrorCode(), record.getLastError());
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

    private QueryWrapper<OutboxEventRecord> publishableQuery(LocalDateTime currentTime, int limit) {
        return new QueryWrapper<OutboxEventRecord>().and(wrapper -> wrapper
                .in("status", OutboxStatus.NEW.name(), OutboxStatus.FAILED.name()).le("next_retry_at", currentTime).or()
                .eq("status", OutboxStatus.PROCESSING.name()).le("claimed_until", currentTime))
                .orderByAsc("shard_id", "created_at").last("LIMIT " + limit);
    }

    private LocalDateTime databaseTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
