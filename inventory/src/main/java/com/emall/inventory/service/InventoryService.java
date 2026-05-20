package com.emall.inventory.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.idempotency.IdempotencyExecutor;
import com.emall.common.idempotency.IdempotencyKey;
import com.emall.common.idempotency.IdempotencyService;
import com.emall.common.idempotency.InMemoryIdempotencyRepository;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InventoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ShardRoutingOperations shardRoutingOperations;
    private final OwnershipGuard ownershipGuard;
    private final BusinessMetrics businessMetrics;
    private final IdempotencyService idempotencyService;

    public InventoryService(InventoryRepository inventoryRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator) {
        this(inventoryRepository, outboxRepository, idGenerator, ShardRoutingOperations.noop(), OwnershipGuard.noop(),
                BusinessMetrics.noop(), localIdempotencyService());
    }

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdempotencyService idempotencyService) {
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
        this.businessMetrics = businessMetrics;
        this.idempotencyService = idempotencyService;
    }

    public InventoryItem get(long skuId) {
        return shardRoutingOperations.execute("inventory_item", skuId, () -> inventoryRepository.findItem(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "inventory item not found")));
    }

    @Transactional
    public InventoryItem addStock(long skuId, int quantity) {
        return shardRoutingOperations.execute("inventory_item", skuId, () -> {
            ownershipGuard.checkWrite("inventory", skuId);
            InventoryItem item =
                    inventoryRepository.findItem(skuId).orElse(new InventoryItem(skuId, 0, 0, 0, Instant.now()));
            return inventoryRepository.saveItem(item.add(quantity));
        });
    }

    @Transactional
    public List<InventoryBucket> initializeBuckets(long skuId, int bucketCount) {
        return shardRoutingOperations.execute("inventory_item", skuId,
                () -> initializeBucketsInShard(skuId, bucketCount));
    }

    private List<InventoryBucket> initializeBucketsInShard(long skuId, int bucketCount) {
        ownershipGuard.checkWrite("inventory", skuId);
        if (bucketCount < 2 || bucketCount > 256) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "bucket count must be between 2 and 256");
        }
        List<InventoryBucket> existingBuckets = inventoryRepository.findBuckets(skuId);
        if (!existingBuckets.isEmpty()) {
            if (existingBuckets.size() > bucketCount) {
                throw new BusinessException(ErrorCode.CONFLICT, "bucket count cannot be reduced online");
            }
            for (int bucketNo = existingBuckets.size(); bucketNo < bucketCount; bucketNo++) {
                inventoryRepository.saveBucket(new InventoryBucket(skuId, bucketNo, 0, 0, 0, Instant.now()));
            }
            return inventoryRepository.findBuckets(skuId);
        }
        InventoryItem item = get(skuId);
        long baseQuantity = item.available() / bucketCount;
        long remainder = item.available() % bucketCount;
        for (int bucketNo = 0; bucketNo < bucketCount; bucketNo++) {
            long quantity = baseQuantity + (bucketNo < remainder ? 1 : 0);
            InventoryBucket bucket = inventoryRepository.findBucket(skuId, bucketNo)
                    .orElse(new InventoryBucket(skuId, bucketNo, 0, 0, 0, Instant.now()));
            inventoryRepository.saveBucket(bucket.add(quantity));
        }
        return inventoryRepository.findBuckets(skuId);
    }

    public List<InventoryBucket> buckets(long skuId) {
        return shardRoutingOperations.execute("inventory_bucket", skuId, () -> inventoryRepository.findBuckets(skuId));
    }

    @Transactional
    public InventoryReservation reserve(String requestId, long skuId, int quantity) {
        IdempotencyKey key = IdempotencyKey.of("inventory", String.valueOf(skuId), requestId, "reserve");
        String requestDigest = idempotencyService.digest("skuId=" + skuId + ",quantity=" + quantity);
        return IdempotencyExecutor.execute(idempotencyService, key, "InventoryReservation", String.valueOf(skuId),
                requestDigest, () -> reserveIdempotent(requestId, skuId, quantity), ignored -> reservation(requestId),
                reservation -> idempotencyService
                        .digest("requestId=" + reservation.requestId() + ",status=" + reservation.status()));
    }

    private InventoryReservation reserveIdempotent(String requestId, long skuId, int quantity) {
        return shardRoutingOperations.execute("inventory_item", skuId, () -> {
            ownershipGuard.checkWrite("inventory", skuId);
            return inventoryRepository.findReservation(requestId)
                    .map(existing -> validateIdempotentReserve(existing, skuId, quantity))
                    .orElseGet(() -> reserveOnce(requestId, skuId, quantity));
        });
    }

    @Transactional
    public InventoryReservation confirm(String requestId) {
        InventoryReservation reservation = reservation(requestId);
        return shardRoutingOperations.execute("inventory_item", reservation.skuId(), () -> confirmInShard(reservation));
    }

    private InventoryReservation confirmInShard(InventoryReservation reservation) {
        ownershipGuard.checkWrite("inventory", reservation.skuId());
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            return reservation;
        }
        if (reservation.status() != ReservationStatus.RESERVED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "reservation cannot be confirmed from " + reservation.status());
        }
        boolean stockUpdated = reservation.bucketNo() == null
                ? inventoryRepository.confirmItem(reservation.skuId(), reservation.quantity())
                : inventoryRepository.confirmBucket(reservation.skuId(), reservation.bucketNo(),
                        reservation.quantity());
        if (!stockUpdated) {
            throw new BusinessException(ErrorCode.CONFLICT, "insufficient reserved stock");
        }
        InventoryReservation confirmed = reservation.confirm();
        if (!inventoryRepository.updateReservationStatus(reservation.requestId(), ReservationStatus.RESERVED,
                confirmed)) {
            throw new BusinessException(ErrorCode.CONFLICT, "reservation status changed during confirm");
        }
        appendEvent(confirmed, EventTypes.INVENTORY_CONFIRMED);
        businessMetrics.increment(BusinessMetricNames.INVENTORY_CONFIRMED, "bucketed",
                reservation.bucketNo() == null ? "false" : "true");
        return confirmed;
    }

    @Transactional
    public InventoryReservation release(String requestId) {
        InventoryReservation reservation = reservation(requestId);
        return shardRoutingOperations.execute("inventory_item", reservation.skuId(), () -> releaseInShard(reservation));
    }

    private InventoryReservation releaseInShard(InventoryReservation reservation) {
        ownershipGuard.checkWrite("inventory", reservation.skuId());
        if (reservation.status() == ReservationStatus.RELEASED) {
            return reservation;
        }
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CONFLICT, "confirmed reservation cannot be released");
        }
        if (reservation.status() == ReservationStatus.REJECTED) {
            return reservation;
        }
        boolean stockUpdated = reservation.bucketNo() == null
                ? inventoryRepository.releaseItem(reservation.skuId(), reservation.quantity())
                : inventoryRepository.releaseBucket(reservation.skuId(), reservation.bucketNo(),
                        reservation.quantity());
        if (!stockUpdated) {
            throw new BusinessException(ErrorCode.CONFLICT, "insufficient reserved stock");
        }
        InventoryReservation released = reservation.release();
        if (!inventoryRepository.updateReservationStatus(reservation.requestId(), ReservationStatus.RESERVED,
                released)) {
            throw new BusinessException(ErrorCode.CONFLICT, "reservation status changed during release");
        }
        appendEvent(released, EventTypes.INVENTORY_RELEASED);
        businessMetrics.increment(BusinessMetricNames.INVENTORY_RELEASED, "bucketed",
                reservation.bucketNo() == null ? "false" : "true");
        return released;
    }

    private InventoryReservation reserveOnce(String requestId, long skuId, int quantity) {
        List<InventoryBucket> buckets = inventoryRepository.findBuckets(skuId);
        if (!buckets.isEmpty()) {
            return reserveBucket(requestId, skuId, quantity);
        }
        InventoryItem item = get(skuId);
        if (item.available() < quantity || !inventoryRepository.reserveItem(skuId, quantity)) {
            businessMetrics.increment(BusinessMetricNames.INVENTORY_REJECTED, "reason", "insufficient_stock");
            return inventoryRepository
                    .saveReservation(InventoryReservation.rejected(requestId, skuId, quantity, "INSUFFICIENT_STOCK"));
        }
        InventoryReservation reservation = inventoryRepository.saveReservation(InventoryReservation.reserved(requestId,
                skuId, quantity, null, Instant.now().plus(Duration.ofMinutes(15))));
        appendEvent(reservation, EventTypes.INVENTORY_RESERVED);
        businessMetrics.increment(BusinessMetricNames.INVENTORY_RESERVED, "bucketed", "false");
        return reservation;
    }

    private InventoryReservation reserveBucket(String requestId, long skuId, int quantity) {
        for (InventoryBucket bucket : inventoryRepository.findBuckets(skuId)) {
            if (bucket.available() >= quantity
                    && inventoryRepository.reserveBucket(skuId, bucket.bucketNo(), quantity)) {
                InventoryReservation reservation =
                        inventoryRepository.saveReservation(InventoryReservation.reserved(requestId, skuId, quantity,
                                bucket.bucketNo(), Instant.now().plus(Duration.ofMinutes(15))));
                appendEvent(reservation, EventTypes.INVENTORY_RESERVED);
                businessMetrics.increment(BusinessMetricNames.INVENTORY_RESERVED, "bucketed", "true");
                return reservation;
            }
        }
        businessMetrics.increment(BusinessMetricNames.INVENTORY_REJECTED, "reason", "insufficient_bucket_stock");
        return inventoryRepository.saveReservation(
                InventoryReservation.rejected(requestId, skuId, quantity, "INSUFFICIENT_BUCKET_STOCK"));
    }

    private InventoryReservation validateIdempotentReserve(InventoryReservation existing, long skuId, int quantity) {
        if (existing.skuId() != skuId || existing.quantity() != quantity) {
            throw new BusinessException(ErrorCode.CONFLICT, "requestId already used by different reservation request");
        }
        return existing;
    }

    private InventoryReservation reservation(String requestId) {
        return inventoryRepository.findReservation(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reservation not found"));
    }

    private void appendEvent(InventoryReservation reservation, String eventType) {
        outboxRepository.save(OutboxEvent.create("inventory-event-" + idGenerator.nextId(), "InventoryReservation",
                reservation.requestId(), eventType,
                Map.of("requestId", reservation.requestId(), "skuId", reservation.skuId(), "quantity",
                        reservation.quantity(), "bucketNo",
                        reservation.bucketNo() == null ? "" : reservation.bucketNo(), "status",
                        reservation.status().name())));
    }

    private static IdempotencyService localIdempotencyService() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                Duration.ofDays(1));
    }
}
