package com.emall.common.idempotency;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;

public abstract class MybatisPlusIdempotencyRepositorySupport implements IdempotencyRepository {
    private final BaseMapper<IdempotencyRecordEntity> mapper;

    protected MybatisPlusIdempotencyRepositorySupport(BaseMapper<IdempotencyRecordEntity> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return Optional.ofNullable(mapper.selectById(key)).map(this::toDomain);
    }

    @Override
    public boolean insertProcessing(IdempotencyRecord record) {
        try {
            mapper.insert(toEntity(record));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public boolean replace(String key, IdempotencyStatus expectedStatus, IdempotencyRecord record) {
        return mapper.update(null, new UpdateWrapper<IdempotencyRecordEntity>().set("request_id", record.requestId())
                .set("business_type", record.businessType()).set("business_id", record.businessId())
                .set("operation", record.operation()).set("owner_id", record.ownerId())
                .set("request_digest", record.requestDigest()).set("response_digest", record.responseDigest())
                .set("status", record.status().name()).set("locked_until", databaseTime(record.lockedUntil()))
                .set("expires_at", databaseTime(record.expiresAt())).set("updated_at", databaseTime(record.updatedAt()))
                .eq("idempotency_key", key).eq("status", expectedStatus.name())) == 1;
    }

    @Override
    public int deleteExpired(Instant now, int limit) {
        return mapper.delete(new QueryWrapper<IdempotencyRecordEntity>().le("expires_at", databaseTime(now))
                .orderByAsc("expires_at").last("LIMIT " + limit));
    }

    private IdempotencyRecordEntity toEntity(IdempotencyRecord record) {
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setKey(record.key());
        entity.setRequestId(record.requestId());
        entity.setBusinessType(record.businessType());
        entity.setBusinessId(record.businessId());
        entity.setOperation(record.operation());
        entity.setOwnerId(record.ownerId());
        entity.setRequestDigest(record.requestDigest());
        entity.setResponseDigest(record.responseDigest());
        entity.setStatus(record.status().name());
        entity.setLockedUntil(databaseTime(record.lockedUntil()));
        entity.setExpiresAt(databaseTime(record.expiresAt()));
        entity.setCreatedAt(databaseTime(record.createdAt()));
        entity.setUpdatedAt(databaseTime(record.updatedAt()));
        return entity;
    }

    private IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
        return new IdempotencyRecord(entity.getKey(), entity.getRequestId(), entity.getBusinessType(),
                entity.getBusinessId(), entity.getOperation(), entity.getOwnerId(), entity.getRequestDigest(),
                entity.getResponseDigest(), IdempotencyStatus.valueOf(entity.getStatus()),
                domainTime(entity.getLockedUntil()), domainTime(entity.getExpiresAt()),
                domainTime(entity.getCreatedAt()), domainTime(entity.getUpdatedAt()));
    }

    private LocalDateTime databaseTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
