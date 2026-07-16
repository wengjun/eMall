package com.emall.common.controlplane;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;

public class MybatisPlusControlPlaneOperationStore implements ControlPlaneOperationStore {
    private static final List<String> CLAIMABLE_STATUSES = List.of(ControlPlaneOperationStatus.PENDING.name(),
            ControlPlaneOperationStatus.APPLYING.name(), ControlPlaneOperationStatus.VERIFYING.name(),
            ControlPlaneOperationStatus.RETRYING.name(), ControlPlaneOperationStatus.ROLLING_BACK.name());

    private final ControlPlaneOperationMapper mapper;
    private final ControlPlaneJson json;

    public MybatisPlusControlPlaneOperationStore(ControlPlaneOperationMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.json = new ControlPlaneJson(objectMapper);
    }

    @Override
    public ControlPlaneOperation insertIfAbsent(ControlPlaneOperation operation) {
        try {
            mapper.insert(toEntity(operation));
            return operation;
        } catch (DuplicateKeyException exception) {
            return findByIdempotencyKey(operation.idempotencyKey()).orElseThrow(() -> exception);
        }
    }

    @Override
    public Optional<ControlPlaneOperation> find(String operationId) {
        return Optional.ofNullable(mapper.selectById(operationId)).map(this::toDomain);
    }

    @Override
    public Optional<ControlPlaneOperation> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(mapper.selectOne(
                new QueryWrapper<ControlPlaneOperationEntity>().eq("idempotency_key", idempotencyKey).last("LIMIT 1")))
                .map(this::toDomain);
    }

    @Override
    public Optional<ControlPlaneOperation> findLatest(String module, String resourceType, String resourceId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<ControlPlaneOperationEntity>()
                .eq("module_name", module).eq("resource_type", resourceType).eq("resource_id", resourceId)
                .orderByDesc("created_at").last("LIMIT 1"))).map(this::toDomain);
    }

    @Override
    public List<ControlPlaneOperation> findClaimable(Instant now, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        QueryWrapper<ControlPlaneOperationEntity> query = new QueryWrapper<ControlPlaneOperationEntity>()
                .in("status", CLAIMABLE_STATUSES).le("next_attempt_at", now)
                .and(condition -> condition.isNull("lease_until").or().le("lease_until", now))
                .orderByAsc("next_attempt_at", "created_at").last("LIMIT " + boundedLimit);
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean claim(String operationId, String owner, Instant leaseUntil, Instant now) {
        return mapper.update(null,
                new UpdateWrapper<ControlPlaneOperationEntity>().set("lease_owner", owner)
                        .set("lease_until", leaseUntil).set("updated_at", now).eq("operation_id", operationId)
                        .in("status", CLAIMABLE_STATUSES)
                        .and(condition -> condition.isNull("lease_until").or().le("lease_until", now))) == 1;
    }

    @Override
    public boolean saveRollbackState(String operationId, String owner, Map<String, Object> rollbackState, Instant now) {
        return mapper.update(null,
                new UpdateWrapper<ControlPlaneOperationEntity>().set("rollback_state", json.write(rollbackState))
                        .set("updated_at", now).eq("operation_id", operationId).eq("lease_owner", owner)
                        .isNull("rollback_state")) == 1;
    }

    @Override
    public boolean transition(String operationId, String owner, ControlPlaneOperationStatus status, int attemptCount,
            Map<String, Object> observedState, String lastError, Instant nextAttemptAt, boolean releaseLease,
            Instant now) {
        UpdateWrapper<ControlPlaneOperationEntity> update =
                new UpdateWrapper<ControlPlaneOperationEntity>().set("status", status.name())
                        .set("attempt_count", attemptCount).set("observed_state", json.write(observedState))
                        .set("last_error", lastError).set("next_attempt_at", nextAttemptAt).set("updated_at", now)
                        .eq("operation_id", operationId).eq("lease_owner", owner);
        if (releaseLease) {
            update.set("lease_owner", null).set("lease_until", null);
        }
        return mapper.update(null, update) == 1;
    }

    private ControlPlaneOperationEntity toEntity(ControlPlaneOperation operation) {
        ControlPlaneOperationEntity entity = new ControlPlaneOperationEntity();
        entity.setOperationId(operation.operationId());
        entity.setIdempotencyKey(operation.idempotencyKey());
        entity.setModuleName(operation.module());
        entity.setTargetType(operation.target().name());
        entity.setActionName(operation.action());
        entity.setResourceType(operation.resourceType());
        entity.setResourceId(operation.resourceId());
        entity.setDesiredState(json.write(operation.desiredState()));
        entity.setDesiredDigest(operation.desiredDigest());
        entity.setRollbackState(json.write(operation.rollbackState()));
        entity.setObservedState(json.write(operation.observedState()));
        entity.setStatus(operation.status().name());
        entity.setAttemptCount(operation.attemptCount());
        entity.setMaxAttempts(operation.maxAttempts());
        entity.setNextAttemptAt(operation.nextAttemptAt());
        entity.setLeaseOwner(operation.leaseOwner());
        entity.setLeaseUntil(operation.leaseUntil());
        entity.setLastError(operation.lastError());
        entity.setCreatedAt(operation.createdAt());
        entity.setUpdatedAt(operation.updatedAt());
        return entity;
    }

    private ControlPlaneOperation toDomain(ControlPlaneOperationEntity entity) {
        return new ControlPlaneOperation(entity.getOperationId(), entity.getIdempotencyKey(), entity.getModuleName(),
                ControlPlaneTarget.valueOf(entity.getTargetType()), entity.getActionName(), entity.getResourceType(),
                entity.getResourceId(), json.read(entity.getDesiredState()), entity.getDesiredDigest(),
                json.read(entity.getRollbackState()), json.read(entity.getObservedState()),
                ControlPlaneOperationStatus.valueOf(entity.getStatus()), entity.getAttemptCount(),
                entity.getMaxAttempts(), entity.getNextAttemptAt(), entity.getLeaseOwner(), entity.getLeaseUntil(),
                entity.getLastError(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
