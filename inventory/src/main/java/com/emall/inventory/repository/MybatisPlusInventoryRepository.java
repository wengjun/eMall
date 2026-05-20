package com.emall.inventory.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryReservation;
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

    public MybatisPlusInventoryRepository(InventoryItemMapper itemMapper, InventoryBucketMapper bucketMapper,
            InventoryReservationMapper reservationMapper) {
        this.itemMapper = itemMapper;
        this.bucketMapper = bucketMapper;
        this.reservationMapper = reservationMapper;
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
                            .set("updated_at", entity.getUpdatedAt()).eq("sku_id", entity.getSkuId()));
        }
        return item;
    }

    @Override
    public Optional<InventoryItem> findItem(long skuId) {
        return Optional.ofNullable(itemMapper.selectById(skuId)).map(this::toItem);
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
        return bucketMapper
                .selectList(new QueryWrapper<InventoryBucketEntity>().eq("sku_id", skuId).orderByAsc("bucket_no"))
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
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)).eq("sku_id", skuId)
                        .apply("total - reserved - sold >= {0}", quantity)) == 1;
    }

    @Override
    public boolean confirmItem(long skuId, int quantity) {
        return itemMapper.update(null,
                new UpdateWrapper<InventoryItemEntity>().setSql("reserved = reserved - {0}", quantity)
                        .setSql("sold = sold + {0}", quantity).set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                        .eq("sku_id", skuId).ge("reserved", quantity)) == 1;
    }

    @Override
    public boolean releaseItem(long skuId, int quantity) {
        return itemMapper.update(null,
                new UpdateWrapper<InventoryItemEntity>().setSql("reserved = reserved - {0}", quantity)
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)).eq("sku_id", skuId)
                        .ge("reserved", quantity)) == 1;
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
                .selectList(
                        new QueryWrapper<InventoryReservationEntity>().eq("status", ReservationStatus.RESERVED.name())
                                .le("expires_at", databaseTime(now)).orderByAsc("expires_at").last("LIMIT " + limit))
                .stream().map(this::toReservation).toList();
    }

    private InventoryItemEntity toItemEntity(InventoryItem item) {
        InventoryItemEntity entity = new InventoryItemEntity();
        entity.setSkuId(item.skuId());
        entity.setTotal(item.total());
        entity.setReserved(item.reserved());
        entity.setSold(item.sold());
        entity.setUpdatedAt(databaseTime(item.updatedAt()));
        return entity;
    }

    private InventoryItem toItem(InventoryItemEntity entity) {
        return new InventoryItem(entity.getSkuId(), entity.getTotal(), entity.getReserved(), entity.getSold(),
                domainTime(entity.getUpdatedAt()));
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

    private LocalDateTime databaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC);
    }
}
