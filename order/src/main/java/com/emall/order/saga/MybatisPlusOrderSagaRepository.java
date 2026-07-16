package com.emall.order.saga;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusOrderSagaRepository implements OrderSagaRepository {
    private final OrderSagaMapper mapper;

    public MybatisPlusOrderSagaRepository(OrderSagaMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrderCreateSaga save(OrderCreateSaga saga) {
        OrderSagaEntity entity = toEntity(saga);
        if (saga.version() == 0L) {
            try {
                mapper.insert(entity);
            } catch (DuplicateKeyException ex) {
                return findByRequestId(saga.requestId()).orElseThrow(() -> ex);
            }
            return saga;
        }
        int updated = mapper.update(null, new UpdateWrapper<OrderSagaEntity>().set("coupon_id", entity.getCouponId())
                .set("inventory_reservation_id", entity.getInventoryReservationId()).set("stage", entity.getStage())
                .set("status", entity.getStatus()).set("attempts", entity.getAttempts())
                .set("version", entity.getVersion()).set("last_error", entity.getLastError())
                .set("next_retry_at", entity.getNextRetryAt()).set("updated_at", entity.getUpdatedAt())
                .eq("saga_id", entity.getSagaId()).eq("version", saga.version() - 1));
        if (updated != 1) {
            throw new OrderSagaConcurrencyException(saga.requestId(), saga.version() - 1);
        }
        return saga;
    }

    @Override
    public Optional<OrderCreateSaga> findByRequestId(String requestId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<OrderSagaEntity>().eq("request_id", requestId)))
                .map(this::toDomain);
    }

    @Override
    public List<OrderCreateSaga> findRecoverable(Instant staleBefore, Instant retryBefore, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        LocalDateTime stale = databaseTime(staleBefore);
        LocalDateTime retry = databaseTime(retryBefore);
        return mapper
                .selectList(
                        new QueryWrapper<OrderSagaEntity>()
                                .and(query -> query
                                        .and(running -> running.eq("status", OrderSagaStatus.RUNNING.name())
                                                .lt("updated_at", stale))
                                        .or(recovering -> recovering
                                                .in("status", OrderSagaStatus.COMPENSATING.name(),
                                                        OrderSagaStatus.MANUAL_REVIEW.name())
                                                .le("next_retry_at", retry)))
                                .orderByAsc("updated_at").last("LIMIT " + boundedLimit))
                .stream().map(this::toDomain).toList();
    }

    private OrderSagaEntity toEntity(OrderCreateSaga saga) {
        OrderSagaEntity entity = new OrderSagaEntity();
        entity.setSagaId(saga.sagaId());
        entity.setRequestId(saga.requestId());
        entity.setOrderId(saga.orderId());
        entity.setUserId(saga.userId());
        entity.setSkuId(saga.skuId());
        entity.setCouponId(saga.couponId());
        entity.setInventoryReservationId(saga.inventoryReservationId());
        entity.setStage(saga.stage().name());
        entity.setStatus(saga.status().name());
        entity.setAttempts(saga.attempts());
        entity.setVersion(saga.version());
        entity.setLastError(saga.lastError());
        entity.setNextRetryAt(databaseTime(saga.nextRetryAt()));
        entity.setCreatedAt(databaseTime(saga.createdAt()));
        entity.setUpdatedAt(databaseTime(saga.updatedAt()));
        return entity;
    }

    private OrderCreateSaga toDomain(OrderSagaEntity entity) {
        return new OrderCreateSaga(entity.getSagaId(), entity.getRequestId(), entity.getOrderId(), entity.getUserId(),
                entity.getSkuId(), entity.getCouponId(), entity.getInventoryReservationId(),
                OrderSagaStage.valueOf(entity.getStage()), OrderSagaStatus.valueOf(entity.getStatus()),
                entity.getAttempts(), entity.getVersion(), entity.getLastError(), domainTime(entity.getNextRetryAt()),
                domainTime(entity.getCreatedAt()), domainTime(entity.getUpdatedAt()));
    }

    private LocalDateTime databaseTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
