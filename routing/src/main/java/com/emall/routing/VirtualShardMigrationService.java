package com.emall.routing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.PhysicalShardPlacement;
import com.emall.common.sharding.ShardMigrationState;
import com.emall.common.sharding.VirtualShardPlacement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VirtualShardMigrationService {
    private final VirtualShardMigrationMapper mapper;
    private final VirtualShardMigrationAuditMapper auditMapper;
    private final VirtualShardMigrationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public VirtualShardMigrationService(VirtualShardMigrationMapper mapper,
            VirtualShardMigrationAuditMapper auditMapper, VirtualShardMigrationProperties properties,
            ObjectMapper objectMapper) {
        this(mapper, auditMapper, properties, objectMapper, Clock.systemUTC());
    }

    VirtualShardMigrationService(VirtualShardMigrationMapper mapper, VirtualShardMigrationAuditMapper auditMapper,
            VirtualShardMigrationProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.mapper = mapper;
        this.auditMapper = auditMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        properties.validate();
    }

    @Transactional(readOnly = true)
    public Optional<VirtualShardPlacement> resolve(String namespace, int virtualShard) {
        validateCoordinate(namespace, virtualShard);
        return Optional.ofNullable(mapper.selectById(placementId(namespace, virtualShard))).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public List<VirtualShardPlacement> list(String namespace) {
        validateNamespace(namespace);
        return mapper
                .selectList(new QueryWrapper<VirtualShardMigrationEntity>().eq("namespace", namespace)
                        .orderByAsc("virtual_shard").last("LIMIT " + properties.getVirtualShardCount()))
                .stream().map(this::toDomain).toList();
    }

    @Transactional
    public VirtualShardPlacement start(String namespace, int virtualShard, PhysicalShardPlacement source,
            PhysicalShardPlacement target, Long expectedVersion, String operator) {
        validateCoordinate(namespace, virtualShard);
        requireOperator(operator);
        if (source == null || target == null || source.equals(target)) {
            throw badRequest("source and target placements must be present and different");
        }
        String id = placementId(namespace, virtualShard);
        VirtualShardMigrationEntity current = mapper.selectById(id);
        Instant now = databasePrecision(clock.instant());
        if (current == null) {
            if (expectedVersion != null && expectedVersion != 1L) {
                throw conflict("virtual shard mapping version changed; reload before retrying");
            }
            VirtualShardMigrationEntity created = new VirtualShardMigrationEntity();
            created.setPlacementId(id);
            created.setNamespace(namespace);
            created.setVirtualShard(virtualShard);
            created.setMappingVersion(2L);
            created.setEpoch(1L);
            created.setMigrationId(UUID.randomUUID().toString());
            created.setState(ShardMigrationState.PREPARING.name());
            created.setPrimaryPlacementJson(writePlacement(source));
            created.setTargetPlacementJson(writePlacement(target));
            created.setCutoverCompleted(false);
            created.setOperatorName(operator);
            created.setCreatedAt(databaseTime(now));
            created.setUpdatedAt(databaseTime(now));
            try {
                mapper.insert(created);
            } catch (DuplicateKeyException exception) {
                throw conflict("virtual shard migration was created concurrently");
            }
            audit(created, ShardMigrationState.STABLE, 1L, operator);
            return toDomain(created);
        }
        ShardMigrationState state = state(current);
        if (state != ShardMigrationState.STABLE && state != ShardMigrationState.ROLLED_BACK) {
            throw conflict("virtual shard already has an active migration");
        }
        requireVersion(current, expectedVersion);
        if (!readPlacement(current.getPrimaryPlacementJson()).equals(source)) {
            throw conflict("migration source does not match the authoritative placement");
        }
        long previousVersion = current.getMappingVersion();
        current.setMigrationId(UUID.randomUUID().toString());
        current.setState(ShardMigrationState.PREPARING.name());
        current.setTargetPlacementJson(writePlacement(target));
        current.setCutoverCompleted(false);
        current.setCutoverNotBefore(null);
        current.setObservationUntil(null);
        clearEvidence(current);
        current.setFailureReason(null);
        persist(current, previousVersion, state, operator);
        return toDomain(current);
    }

    @Transactional
    public VirtualShardPlacement advance(String namespace, int virtualShard, long expectedVersion,
            ShardMigrationState targetState, VirtualShardMigrationEvidence evidence, String operator) {
        VirtualShardMigrationEntity current = requireCurrent(namespace, virtualShard);
        requireVersion(current, expectedVersion);
        requireOperator(operator);
        ShardMigrationState fromState = state(current);
        requireNextState(fromState, targetState);
        applyEvidence(current, evidence == null ? VirtualShardMigrationEvidence.empty() : evidence);
        long previousVersion = current.getMappingVersion();
        Instant now = databasePrecision(clock.instant());
        switch (targetState) {
            case COPYING -> requireTarget(current);
            case CATCHING_UP -> requireCopyCompleted(current);
            case VERIFYING -> requireCdcCaughtUp(current);
            case CUTOVER_PENDING -> {
                requireVerified(current);
                current.setCutoverNotBefore(databaseTime(now.plus(properties.getMinimumCutoverDelay())));
            }
            case OBSERVING -> {
                requireDelayElapsed(current.getCutoverNotBefore(), "cutover propagation delay has not elapsed");
                swapPlacements(current);
                current.setEpoch(current.getEpoch() + 1);
                current.setCutoverCompleted(true);
                current.setObservationUntil(databaseTime(now.plus(properties.getObservationPeriod())));
            }
            case CLEANUP ->
                requireDelayElapsed(current.getObservationUntil(), "post-cutover observation period has not elapsed");
            case STABLE -> finishMigration(current);
            default -> {
            }
        }
        current.setState(targetState.name());
        persist(current, previousVersion, fromState, operator);
        return toDomain(current);
    }

    @Transactional
    public VirtualShardPlacement fail(String namespace, int virtualShard, long expectedVersion, String reason,
            String operator) {
        VirtualShardMigrationEntity current = requireCurrent(namespace, virtualShard);
        requireVersion(current, expectedVersion);
        requireOperator(operator);
        if (!StringUtils.hasText(reason) || !state(current).migrationActive()) {
            throw badRequest("only an active migration can be failed with a reason");
        }
        ShardMigrationState fromState = state(current);
        long previousVersion = current.getMappingVersion();
        current.setState(ShardMigrationState.FAILED.name());
        current.setFailureReason(limit(reason, 512));
        persist(current, previousVersion, fromState, operator);
        return toDomain(current);
    }

    @Transactional
    public VirtualShardPlacement rollback(String namespace, int virtualShard, long expectedVersion, String operator) {
        VirtualShardMigrationEntity current = requireCurrent(namespace, virtualShard);
        requireVersion(current, expectedVersion);
        requireOperator(operator);
        ShardMigrationState fromState = state(current);
        if (fromState == ShardMigrationState.STABLE || fromState == ShardMigrationState.ROLLED_BACK) {
            throw conflict("virtual shard has no migration to roll back");
        }
        long previousVersion = current.getMappingVersion();
        Instant now = databasePrecision(clock.instant());
        if (!Boolean.TRUE.equals(current.getCutoverCompleted())) {
            current.setState(ShardMigrationState.ROLLED_BACK.name());
            current.setTargetPlacementJson(null);
            current.setCutoverNotBefore(null);
            current.setObservationUntil(null);
        } else if (fromState != ShardMigrationState.ROLLBACK_PENDING) {
            requireTarget(current);
            current.setState(ShardMigrationState.ROLLBACK_PENDING.name());
            current.setCutoverNotBefore(databaseTime(now.plus(properties.getMinimumCutoverDelay())));
        } else {
            requireDelayElapsed(current.getCutoverNotBefore(), "rollback propagation delay has not elapsed");
            swapPlacements(current);
            current.setEpoch(current.getEpoch() + 1);
            current.setState(ShardMigrationState.ROLLED_BACK.name());
            current.setTargetPlacementJson(null);
            current.setCutoverCompleted(false);
            current.setCutoverNotBefore(null);
            current.setObservationUntil(null);
        }
        persist(current, previousVersion, fromState, operator);
        return toDomain(current);
    }

    private void persist(VirtualShardMigrationEntity entity, long previousVersion, ShardMigrationState fromState,
            String operator) {
        entity.setMappingVersion(previousVersion + 1);
        entity.setOperatorName(operator);
        entity.setUpdatedAt(databaseTime(databasePrecision(clock.instant())));
        int updated = mapper.update(null,
                new UpdateWrapper<VirtualShardMigrationEntity>().set("mapping_version", entity.getMappingVersion())
                        .set("epoch", entity.getEpoch()).set("migration_id", entity.getMigrationId())
                        .set("state", entity.getState()).set("primary_placement_json", entity.getPrimaryPlacementJson())
                        .set("target_placement_json", entity.getTargetPlacementJson())
                        .set("cutover_not_before", entity.getCutoverNotBefore())
                        .set("observation_until", entity.getObservationUntil())
                        .set("copy_cursor", entity.getCopyCursor()).set("source_row_count", entity.getSourceRowCount())
                        .set("target_row_count", entity.getTargetRowCount())
                        .set("source_checksum", entity.getSourceChecksum())
                        .set("target_checksum", entity.getTargetChecksum()).set("cdc_lag", entity.getCdcLag())
                        .set("cutover_completed", entity.getCutoverCompleted()).set("operator_name", operator)
                        .set("failure_reason", entity.getFailureReason()).set("updated_at", entity.getUpdatedAt())
                        .eq("placement_id", entity.getPlacementId()).eq("mapping_version", previousVersion));
        if (updated != 1) {
            throw conflict("virtual shard mapping changed concurrently; reload before retrying");
        }
        audit(entity, fromState, previousVersion, operator);
    }

    private void audit(VirtualShardMigrationEntity entity, ShardMigrationState fromState, long previousVersion,
            String operator) {
        VirtualShardMigrationAuditEntity audit = new VirtualShardMigrationAuditEntity();
        audit.setPlacementId(entity.getPlacementId());
        audit.setMigrationId(entity.getMigrationId());
        audit.setFromState(fromState == null ? null : fromState.name());
        audit.setToState(entity.getState());
        audit.setPreviousVersion(previousVersion);
        audit.setNewVersion(entity.getMappingVersion());
        audit.setEpoch(entity.getEpoch());
        audit.setOperatorName(operator);
        audit.setSnapshotJson(writeJson(entity));
        audit.setCreatedAt(entity.getUpdatedAt());
        auditMapper.insert(audit);
    }

    private void requireNextState(ShardMigrationState current, ShardMigrationState target) {
        ShardMigrationState expected = switch (current) {
            case PREPARING -> ShardMigrationState.COPYING;
            case COPYING -> ShardMigrationState.CATCHING_UP;
            case CATCHING_UP -> ShardMigrationState.VERIFYING;
            case VERIFYING -> ShardMigrationState.CUTOVER_PENDING;
            case CUTOVER_PENDING -> ShardMigrationState.OBSERVING;
            case OBSERVING -> ShardMigrationState.CLEANUP;
            case CLEANUP -> ShardMigrationState.STABLE;
            default -> null;
        };
        if (target == null || target != expected) {
            throw conflict("invalid migration transition from " + current + " to " + target);
        }
    }

    private void applyEvidence(VirtualShardMigrationEntity entity, VirtualShardMigrationEvidence evidence) {
        if (StringUtils.hasText(evidence.copyCursor())) {
            entity.setCopyCursor(limit(evidence.copyCursor(), 512));
        }
        if (evidence.sourceRowCount() != null) {
            entity.setSourceRowCount(evidence.sourceRowCount());
        }
        if (evidence.targetRowCount() != null) {
            entity.setTargetRowCount(evidence.targetRowCount());
        }
        if (StringUtils.hasText(evidence.sourceChecksum())) {
            entity.setSourceChecksum(limit(evidence.sourceChecksum(), 128));
        }
        if (StringUtils.hasText(evidence.targetChecksum())) {
            entity.setTargetChecksum(limit(evidence.targetChecksum(), 128));
        }
        if (evidence.cdcLag() != null) {
            entity.setCdcLag(evidence.cdcLag());
        }
    }

    private void requireCopyCompleted(VirtualShardMigrationEntity entity) {
        requireTarget(entity);
        if (!StringUtils.hasText(entity.getCopyCursor()) || entity.getSourceRowCount() == null
                || entity.getTargetRowCount() == null) {
            throw conflict("copy cursor and source/target row counts are required before CDC catch-up");
        }
    }

    private void requireCdcCaughtUp(VirtualShardMigrationEntity entity) {
        requireCopyCompleted(entity);
        if (!Objects.equals(entity.getCdcLag(), 0L)) {
            throw conflict("CDC lag must be zero before verification");
        }
    }

    private void requireVerified(VirtualShardMigrationEntity entity) {
        requireCdcCaughtUp(entity);
        if (!Objects.equals(entity.getSourceRowCount(), entity.getTargetRowCount())
                || !StringUtils.hasText(entity.getSourceChecksum())
                || !Objects.equals(entity.getSourceChecksum(), entity.getTargetChecksum())) {
            throw conflict("row counts and checksums must match before cutover");
        }
    }

    private void requireTarget(VirtualShardMigrationEntity entity) {
        if (!StringUtils.hasText(entity.getTargetPlacementJson())) {
            throw conflict("migration target placement is missing");
        }
    }

    private void requireDelayElapsed(LocalDateTime deadline, String message) {
        if (deadline == null || clock.instant().isBefore(domainTime(deadline))) {
            throw conflict(message);
        }
    }

    private void swapPlacements(VirtualShardMigrationEntity entity) {
        requireTarget(entity);
        String previousPrimary = entity.getPrimaryPlacementJson();
        entity.setPrimaryPlacementJson(entity.getTargetPlacementJson());
        entity.setTargetPlacementJson(previousPrimary);
    }

    private void finishMigration(VirtualShardMigrationEntity entity) {
        entity.setTargetPlacementJson(null);
        entity.setMigrationId(null);
        entity.setCutoverNotBefore(null);
        entity.setObservationUntil(null);
        entity.setCutoverCompleted(false);
        clearEvidence(entity);
        entity.setFailureReason(null);
    }

    private void clearEvidence(VirtualShardMigrationEntity entity) {
        entity.setCopyCursor(null);
        entity.setSourceRowCount(null);
        entity.setTargetRowCount(null);
        entity.setSourceChecksum(null);
        entity.setTargetChecksum(null);
        entity.setCdcLag(null);
    }

    private VirtualShardMigrationEntity requireCurrent(String namespace, int virtualShard) {
        validateCoordinate(namespace, virtualShard);
        VirtualShardMigrationEntity current = mapper.selectById(placementId(namespace, virtualShard));
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "virtual shard migration not found");
        }
        return current;
    }

    private void requireVersion(VirtualShardMigrationEntity entity, Long expectedVersion) {
        if (expectedVersion == null || !Objects.equals(entity.getMappingVersion(), expectedVersion)) {
            throw conflict("virtual shard mapping version changed; reload before retrying");
        }
    }

    private void validateCoordinate(String namespace, int virtualShard) {
        validateNamespace(namespace);
        if (virtualShard < 0 || virtualShard >= properties.getVirtualShardCount()) {
            throw badRequest("virtual shard is outside the configured range");
        }
    }

    private void validateNamespace(String namespace) {
        if (namespace == null || !namespace.matches("[a-z0-9-]{1,64}")) {
            throw badRequest("virtual shard namespace is invalid");
        }
    }

    private void requireOperator(String operator) {
        if (!StringUtils.hasText(operator) || operator.length() > 128) {
            throw badRequest("migration operator is required and must not exceed 128 characters");
        }
    }

    private ShardMigrationState state(VirtualShardMigrationEntity entity) {
        try {
            return ShardMigrationState.valueOf(entity.getState());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("stored virtual shard migration state is invalid", exception);
        }
    }

    private VirtualShardPlacement toDomain(VirtualShardMigrationEntity entity) {
        return new VirtualShardPlacement(entity.getNamespace(), entity.getVirtualShard(), entity.getMappingVersion(),
                entity.getEpoch(), state(entity), readPlacement(entity.getPrimaryPlacementJson()),
                readNullablePlacement(entity.getTargetPlacementJson()), domainTime(entity.getCutoverNotBefore()),
                domainTime(entity.getUpdatedAt()));
    }

    private String writePlacement(PhysicalShardPlacement placement) {
        return writeJson(placement);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize virtual shard migration state", exception);
        }
    }

    private PhysicalShardPlacement readNullablePlacement(String json) {
        return StringUtils.hasText(json) ? readPlacement(json) : null;
    }

    private PhysicalShardPlacement readPlacement(String json) {
        try {
            return objectMapper.readValue(json, PhysicalShardPlacement.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored virtual shard placement is invalid", exception);
        }
    }

    private String placementId(String namespace, int virtualShard) {
        return namespace + ':' + virtualShard;
    }

    private LocalDateTime databaseTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private Instant databasePrecision(Instant value) {
        return value.truncatedTo(ChronoUnit.MICROS);
    }

    private String limit(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
