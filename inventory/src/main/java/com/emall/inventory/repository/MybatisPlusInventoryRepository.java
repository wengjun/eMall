package com.emall.inventory.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.common.persistence.BoundedQuery;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryLedgerOperation;
import com.emall.inventory.domain.InventoryMode;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.InventoryStockLedger;
import com.emall.inventory.domain.InventoryStockSummary;
import com.emall.inventory.domain.ReservationStatus;
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
public class MybatisPlusInventoryRepository implements InventoryRepository {
    private final InventoryItemMapper itemMapper;
    private final InventoryBucketMapper bucketMapper;
    private final InventoryReservationMapper reservationMapper;
    private final InventoryStockLedgerMapper stockLedgerMapper;

    public MybatisPlusInventoryRepository(InventoryItemMapper itemMapper, InventoryBucketMapper bucketMapper,
            InventoryReservationMapper reservationMapper, InventoryStockLedgerMapper stockLedgerMapper) {
        this.itemMapper = itemMapper;
        this.bucketMapper = bucketMapper;
        this.reservationMapper = reservationMapper;
        this.stockLedgerMapper = stockLedgerMapper;
    }

    @Override
    public InventoryItem saveItem(InventoryItem item) {
        InventoryItemEntity entity = toItemEntity(item);
        try {
            itemMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            itemMapper.update(null,
                    new UpdateWrapper<InventoryItemEntity>().set("total", entity.getTotal())
                            .set("reserved", entity.getReserved()).set("sold", entity.getSold())
                            .set("inventory_mode", entity.getMode()).set("version", entity.getVersion())
                            .set("updated_at", entity.getUpdatedAt()).eq("sku_id", entity.getSkuId()));
        }
        return item;
    }

    @Override
    public Optional<InventoryItem> findItem(long skuId) {
        return Optional.ofNullable(itemMapper.selectById(skuId)).map(this::toItem);
    }

    @Override
    public Optional<InventoryItem> findItemForUpdate(long skuId) {
        return Optional
                .ofNullable(itemMapper
                        .selectOne(new QueryWrapper<InventoryItemEntity>().eq("sku_id", skuId).last("FOR UPDATE")))
                .map(this::toItem);
    }

    @Override
    public InventoryItem ensureItem(long skuId) {
        InventoryItem item = new InventoryItem(skuId, 0, 0, 0, Instant.now());
        try {
            itemMapper.insert(toItemEntity(item));
            return item;
        } catch (DuplicateKeyException ex) {
            return findItem(skuId).orElseThrow(() -> new IllegalStateException("inventory item disappeared", ex));
        }
    }

    @Override
    public boolean addItemStock(long skuId, int quantity, InventoryMode expectedMode) {
        return itemMapper.update(null,
                new UpdateWrapper<InventoryItemEntity>().setSql("total = total + {0}", quantity)
                        .setSql("version = version + 1").set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                        .eq("sku_id", skuId).eq("inventory_mode", expectedMode.name())) == 1;
    }

    @Override
    public boolean initializeBuckets(InventoryItem expectedItem, List<InventoryBucket> initialBuckets) {
        for (InventoryBucket bucket : initialBuckets) {
            bucketMapper.insert(toBucketEntity(bucket));
        }
        InventoryItem bucketedBase = expectedItem.activateBuckets();
        return itemMapper.update(null, new UpdateWrapper<InventoryItemEntity>().set("total", bucketedBase.total())
                .set("reserved", bucketedBase.reserved()).set("sold", bucketedBase.sold())
                .set("inventory_mode", bucketedBase.mode().name()).set("version", bucketedBase.version())
                .set("updated_at", databaseTime(bucketedBase.updatedAt())).eq("sku_id", expectedItem.skuId())
                .eq("inventory_mode", InventoryMode.SINGLE_ROW.name()).eq("version", expectedItem.version())) == 1;
    }

    @Override
    public boolean createBucketIfAbsent(InventoryBucket bucket) {
        try {
            bucketMapper.insert(toBucketEntity(bucket));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public boolean addBucketStock(long skuId, int bucketNo, int quantity) {
        return bucketMapper.update(null,
                new UpdateWrapper<InventoryBucketEntity>().setSql("total = total + {0}", quantity)
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)).eq("sku_id", skuId)
                        .eq("bucket_no", bucketNo)) == 1;
    }

    @Override
    public InventoryStockSummary summarizeBuckets(long skuId) {
        InventoryBucketSummaryProjection summary = bucketMapper.summarize(skuId);
        return new InventoryStockSummary(value(summary.getTotal()), value(summary.getReserved()),
                value(summary.getSold()), Math.toIntExact(value(summary.getBucketCount())),
                summary.getUpdatedAt() == null ? null : domainTime(summary.getUpdatedAt()));
    }

    @Override
    public InventoryBucket saveBucket(InventoryBucket bucket) {
        InventoryBucketEntity entity = toBucketEntity(bucket);
        try {
            bucketMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            bucketMapper.update(null,
                    new UpdateWrapper<InventoryBucketEntity>().set("total", entity.getTotal())
                            .set("reserved", entity.getReserved()).set("sold", entity.getSold())
                            .set("updated_at", entity.getUpdatedAt()).eq("sku_id", entity.getSkuId())
                            .eq("bucket_no", entity.getBucketNo()));
        }
        return bucket;
    }

    @Override
    public List<InventoryBucket> findBuckets(long skuId) {
        return BoundedQuery
                .firstPage(bucketMapper,
                        new QueryWrapper<InventoryBucketEntity>().eq("sku_id", skuId).orderByAsc("bucket_no"))
                .stream().map(this::toBucket).toList();
    }

    @Override
    public Optional<InventoryBucket> findBucket(long skuId, int bucketNo) {
        return Optional
                .ofNullable(bucketMapper.selectOne(
                        new QueryWrapper<InventoryBucketEntity>().eq("sku_id", skuId).eq("bucket_no", bucketNo)))
                .map(this::toBucket);
    }

    @Override
    public Optional<InventoryBucket> findReservableBucket(long skuId, int quantity) {
        return bucketMapper.selectList(new QueryWrapper<InventoryBucketEntity>().eq("sku_id", skuId)
                .apply("total - reserved - sold >= {0}", quantity).orderByAsc("reserved", "bucket_no").last("LIMIT 1"))
                .stream().findFirst().map(this::toBucket);
    }

    @Override
    public boolean reserveItem(long skuId, int quantity) {
        return itemMapper.update(null,
                new UpdateWrapper<InventoryItemEntity>().setSql("reserved = reserved + {0}", quantity)
                        .setSql("version = version + 1").set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                        .eq("sku_id", skuId).eq("inventory_mode", InventoryMode.SINGLE_ROW.name())
                        .apply("total - reserved - sold >= {0}", quantity)) == 1;
    }

    @Override
    public boolean confirmItem(long skuId, int quantity) {
        return itemMapper.update(null,
                new UpdateWrapper<InventoryItemEntity>().setSql("reserved = reserved - {0}", quantity)
                        .setSql("sold = sold + {0}", quantity).setSql("version = version + 1")
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)).eq("sku_id", skuId)
                        .ge("reserved", quantity)) == 1;
    }

    @Override
    public boolean releaseItem(long skuId, int quantity) {
        return itemMapper.update(null,
                new UpdateWrapper<InventoryItemEntity>().setSql("reserved = reserved - {0}", quantity)
                        .setSql("version = version + 1").set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                        .eq("sku_id", skuId).ge("reserved", quantity)) == 1;
    }

    @Override
    public boolean reserveBucket(long skuId, int bucketNo, int quantity) {
        return bucketMapper.update(null,
                new UpdateWrapper<InventoryBucketEntity>().setSql("reserved = reserved + {0}", quantity)
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)).eq("sku_id", skuId)
                        .eq("bucket_no", bucketNo).apply("total - reserved - sold >= {0}", quantity)) == 1;
    }

    @Override
    public boolean confirmBucket(long skuId, int bucketNo, int quantity) {
        return bucketMapper.update(null,
                new UpdateWrapper<InventoryBucketEntity>().setSql("reserved = reserved - {0}", quantity)
                        .setSql("sold = sold + {0}", quantity).set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                        .eq("sku_id", skuId).eq("bucket_no", bucketNo).ge("reserved", quantity)) == 1;
    }

    @Override
    public boolean releaseBucket(long skuId, int bucketNo, int quantity) {
        return bucketMapper.update(null,
                new UpdateWrapper<InventoryBucketEntity>().setSql("reserved = reserved - {0}", quantity)
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)).eq("sku_id", skuId)
                        .eq("bucket_no", bucketNo).ge("reserved", quantity)) == 1;
    }

    @Override
    public InventoryReservation saveReservation(InventoryReservation reservation) {
        InventoryReservationEntity entity = toReservationEntity(reservation);
        try {
            reservationMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            reservationMapper.update(null,
                    new UpdateWrapper<InventoryReservationEntity>().set("status", entity.getStatus())
                            .set("reason", entity.getReason()).set("updated_at", entity.getUpdatedAt())
                            .eq("request_id", entity.getRequestId()));
        }
        return reservation;
    }

    @Override
    public boolean updateReservationStatus(String requestId, ReservationStatus expectedStatus,
            InventoryReservation reservation) {
        InventoryReservationEntity entity = toReservationEntity(reservation);
        return reservationMapper.update(null,
                new UpdateWrapper<InventoryReservationEntity>().set("status", entity.getStatus())
                        .set("reason", entity.getReason()).set("updated_at", entity.getUpdatedAt())
                        .eq("request_id", requestId).eq("status", expectedStatus.name())) == 1;
    }

    @Override
    public Optional<InventoryReservation> findReservation(String requestId) {
        return Optional.ofNullable(reservationMapper.selectById(requestId)).map(this::toReservation);
    }

    @Override
    public List<InventoryReservation> findExpiredReservations(Instant now, int limit) {
        return reservationMapper
                .selectList(new QueryWrapper<InventoryReservationEntity>()
                        .eq("status", ReservationStatus.RESERVED.name()).le("expires_at", databaseTime(now))
                        .orderByAsc("expires_at").last("LIMIT " + BoundedQuery.limit(limit)))
                .stream().map(this::toReservation).toList();
    }

    @Override
    public boolean appendStockLedger(InventoryStockLedger ledger) {
        try {
            return stockLedgerMapper.insert(toStockLedgerEntity(ledger)) == 1;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public List<InventoryStockLedger> findStockLedger(long skuId, int limit) {
        return stockLedgerMapper
                .selectList(new QueryWrapper<InventoryStockLedgerEntity>().eq("sku_id", skuId)
                        .orderByAsc("created_at", "ledger_id").last("LIMIT " + BoundedQuery.limit(limit)))
                .stream().map(this::toStockLedger).toList();
    }

    private InventoryItemEntity toItemEntity(InventoryItem item) {
        InventoryItemEntity entity = new InventoryItemEntity();
        entity.setSkuId(item.skuId());
        entity.setTotal(item.total());
        entity.setReserved(item.reserved());
        entity.setSold(item.sold());
        entity.setMode(item.mode().name());
        entity.setVersion(item.version());
        entity.setUpdatedAt(databaseTime(item.updatedAt()));
        return entity;
    }

    private InventoryItem toItem(InventoryItemEntity entity) {
        return new InventoryItem(entity.getSkuId(), entity.getTotal(), entity.getReserved(), entity.getSold(),
                InventoryMode.valueOf(entity.getMode()), entity.getVersion(), domainTime(entity.getUpdatedAt()));
    }

    private InventoryBucketEntity toBucketEntity(InventoryBucket bucket) {
        InventoryBucketEntity entity = new InventoryBucketEntity();
        entity.setSkuId(bucket.skuId());
        entity.setBucketNo(bucket.bucketNo());
        entity.setTotal(bucket.total());
        entity.setReserved(bucket.reserved());
        entity.setSold(bucket.sold());
        entity.setUpdatedAt(databaseTime(bucket.updatedAt()));
        return entity;
    }

    private InventoryBucket toBucket(InventoryBucketEntity entity) {
        return new InventoryBucket(entity.getSkuId(), entity.getBucketNo(), entity.getTotal(), entity.getReserved(),
                entity.getSold(), domainTime(entity.getUpdatedAt()));
    }

    private InventoryReservationEntity toReservationEntity(InventoryReservation reservation) {
        InventoryReservationEntity entity = new InventoryReservationEntity();
        entity.setRequestId(reservation.requestId());
        entity.setSkuId(reservation.skuId());
        entity.setQuantity(reservation.quantity());
        entity.setBucketNo(reservation.bucketNo());
        entity.setStatus(reservation.status().name());
        entity.setReason(reservation.reason());
        entity.setExpiresAt(databaseTime(reservation.expiresAt()));
        entity.setCreatedAt(databaseTime(reservation.createdAt()));
        entity.setUpdatedAt(databaseTime(reservation.updatedAt()));
        return entity;
    }

    private InventoryReservation toReservation(InventoryReservationEntity entity) {
        return new InventoryReservation(entity.getRequestId(), entity.getSkuId(), entity.getQuantity(),
                entity.getBucketNo(), ReservationStatus.valueOf(entity.getStatus()), entity.getReason(),
                domainTime(entity.getExpiresAt()), domainTime(entity.getCreatedAt()),
                domainTime(entity.getUpdatedAt()));
    }

    private InventoryStockLedgerEntity toStockLedgerEntity(InventoryStockLedger ledger) {
        InventoryStockLedgerEntity entity = new InventoryStockLedgerEntity();
        entity.setLedgerId(ledger.ledgerId());
        entity.setSkuId(ledger.skuId());
        entity.setRequestId(ledger.requestId());
        entity.setOperation(ledger.operation().name());
        entity.setBucketNo(ledger.bucketNo());
        entity.setTotalDelta(ledger.totalDelta());
        entity.setReservedDelta(ledger.reservedDelta());
        entity.setSoldDelta(ledger.soldDelta());
        entity.setCreatedAt(databaseTime(ledger.createdAt()));
        return entity;
    }

    private InventoryStockLedger toStockLedger(InventoryStockLedgerEntity entity) {
        return new InventoryStockLedger(entity.getLedgerId(), entity.getSkuId(), entity.getRequestId(),
                InventoryLedgerOperation.valueOf(entity.getOperation()), entity.getBucketNo(), entity.getTotalDelta(),
                entity.getReservedDelta(), entity.getSoldDelta(), domainTime(entity.getCreatedAt()));
    }

    private LocalDateTime databaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC);
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }
}
