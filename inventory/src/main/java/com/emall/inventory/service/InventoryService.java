package com.emall.inventory.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.InventoryReservationEventPayload;
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
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryLedgerOperation;
import com.emall.inventory.domain.InventoryMode;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.InventoryStockLedger;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InventoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;
    private final OwnershipGuard ownershipGuard;
    private final BusinessMetrics businessMetrics;
    private final IdempotencyService idempotencyService;

    public InventoryService(InventoryRepository inventoryRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator) {
        this(inventoryRepository, outboxRepository, idGenerator, ShardRoutingOperations.noop(), OwnershipGuard.noop(),
                BusinessMetrics.noop(), localIdempotencyService(), ShardRouteIndex.local());
    }

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdempotencyService idempotencyService,
            ShardRouteIndex shardRouteIndex) {
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.shardRoutingOperations = shardRoutingOperations;
        this.shardRouteIndex = shardRouteIndex;
        this.ownershipGuard = ownershipGuard;
        this.businessMetrics = businessMetrics;
        this.idempotencyService = idempotencyService;
    }

    public InventoryItem get(long skuId) {
        return shardRoutingOperations.executeRead("inventory_item", skuId, () -> getInShard(skuId));
    }

    @Transactional
    public InventoryItem addStock(String requestId, long skuId, int quantity) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "stock requestId must not be blank");
        }
        IdempotencyKey key = IdempotencyKey.of("inventory-stock", String.valueOf(skuId), requestId, "add");
        String requestDigest = idempotencyService.digest("skuId=" + skuId + ",quantity=" + quantity);
        return shardRoutingOperations.execute("inventory_item", skuId,
                () -> IdempotencyExecutor.execute(idempotencyService, key, "InventoryStock", String.valueOf(skuId),
                        requestDigest, () -> addStockOnce(requestId, skuId, quantity), ignored -> getInShard(skuId),
                        item -> idempotencyService.digest("skuId=" + item.skuId() + ",total=" + item.total())));
    }

    private InventoryItem addStockOnce(String requestId, long skuId, int quantity) {
        ownershipGuard.checkWrite("inventory", skuId);
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "stock quantity must be positive");
        }
        InventoryItem item = inventoryRepository.ensureItem(skuId);
        if (item.mode() == InventoryMode.SINGLE_ROW
                && inventoryRepository.addItemStock(skuId, quantity, InventoryMode.SINGLE_ROW)) {
            appendStockLedger("stock:" + skuId + ":" + requestId, skuId, requestId,
                    InventoryLedgerOperation.STOCK_ADDED, null, quantity, 0, 0);
            return getInShard(skuId);
        }
        item = requireItem(skuId);
        if (item.mode() != InventoryMode.BUCKETED) {
            throw new BusinessException(ErrorCode.CONFLICT, "inventory mode changed during stock update");
        }
        List<InventoryBucket> buckets = inventoryRepository.findBuckets(skuId);
        if (buckets.isEmpty()) {
            throw new IllegalStateException("bucketed inventory has no buckets for skuId=" + skuId);
        }
        InventoryBucket bucket = buckets.get(Math.floorMod(requestId.hashCode(), buckets.size()));
        if (!inventoryRepository.addBucketStock(skuId, bucket.bucketNo(), quantity)) {
            throw new BusinessException(ErrorCode.CONFLICT, "inventory bucket changed during stock update");
        }
        appendStockLedger("stock:" + skuId + ":" + requestId, skuId, requestId, InventoryLedgerOperation.STOCK_ADDED,
                bucket.bucketNo(), quantity, 0, 0);
        return getInShard(skuId);
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
        InventoryItem item = inventoryRepository.findItemForUpdate(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "inventory item not found"));
        List<InventoryBucket> existingBuckets = inventoryRepository.findBuckets(skuId);
        if (item.mode() == InventoryMode.BUCKETED) {
            if (existingBuckets.isEmpty()) {
                throw new IllegalStateException("bucketed inventory has no buckets for skuId=" + skuId);
            }
            if (existingBuckets.size() > bucketCount) {
                throw new BusinessException(ErrorCode.CONFLICT, "bucket count cannot be reduced online");
            }
            for (int bucketNo = 0; bucketNo < bucketCount; bucketNo++) {
                inventoryRepository.createBucketIfAbsent(new InventoryBucket(skuId, bucketNo, 0, 0, 0, Instant.now()));
            }
            return inventoryRepository.findBuckets(skuId);
        }
        if (!existingBuckets.isEmpty()) {
            throw new IllegalStateException("single-row inventory unexpectedly contains buckets for skuId=" + skuId);
        }
        long baseQuantity = item.available() / bucketCount;
        long remainder = item.available() % bucketCount;
        List<InventoryBucket> initialBuckets = new java.util.ArrayList<>(bucketCount);
        for (int bucketNo = 0; bucketNo < bucketCount; bucketNo++) {
            long quantity = baseQuantity + (bucketNo < remainder ? 1 : 0);
            initialBuckets.add(new InventoryBucket(skuId, bucketNo, quantity, 0, 0, Instant.now()));
        }
        if (!inventoryRepository.initializeBuckets(item, initialBuckets)) {
            throw new BusinessException(ErrorCode.CONFLICT, "inventory changed while buckets were initialized");
        }
        String modeRequestId = "mode-switch:" + skuId + ":" + item.version();
        appendStockLedger(modeRequestId, skuId, modeRequestId, InventoryLedgerOperation.BUCKETS_ACTIVATED, null, 0, 0,
                0);
        return inventoryRepository.findBuckets(skuId);
    }

    public List<InventoryBucket> buckets(long skuId) {
        return shardRoutingOperations.executeRead("inventory_bucket", skuId,
                () -> inventoryRepository.findBuckets(skuId));
    }

    @Transactional
    public InventoryReservation reserve(String requestId, long skuId, int quantity) {
        IdempotencyKey key = IdempotencyKey.of("inventory", String.valueOf(skuId), requestId, "reserve");
        String requestDigest = idempotencyService.digest("skuId=" + skuId + ",quantity=" + quantity);
        return shardRoutingOperations.execute("inventory_item", skuId,
                () -> IdempotencyExecutor.execute(idempotencyService, key, "InventoryReservation",
                        String.valueOf(skuId), requestDigest, () -> reserveIdempotent(requestId, skuId, quantity),
                        ignored -> reservation(requestId), reservation -> idempotencyService
                                .digest("requestId=" + reservation.requestId() + ",status=" + reservation.status())));
    }

    private InventoryReservation reserveIdempotent(String requestId, long skuId, int quantity) {
        return shardRoutingOperations.execute("inventory_item", skuId, () -> {
            ownershipGuard.checkWrite("inventory", skuId);
            InventoryReservation reservation = inventoryRepository.findReservation(requestId)
                    .map(existing -> validateIdempotentReserve(existing, skuId, quantity))
                    .orElseGet(() -> reserveOnce(requestId, skuId, quantity));
            shardRouteIndex.bindUniqueTransactional("inventory-reservation", reservation.requestId(),
                    reservation.skuId());
            return reservation;
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
        appendStockLedger("confirm:" + reservation.requestId(), reservation.skuId(), reservation.requestId(),
                InventoryLedgerOperation.STOCK_CONFIRMED, reservation.bucketNo(), 0, -reservation.quantity(),
                reservation.quantity());
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
        appendStockLedger("release:" + reservation.requestId(), reservation.skuId(), reservation.requestId(),
                InventoryLedgerOperation.STOCK_RELEASED, reservation.bucketNo(), 0, -reservation.quantity(), 0);
        appendEvent(released, EventTypes.INVENTORY_RELEASED);
        businessMetrics.increment(BusinessMetricNames.INVENTORY_RELEASED, "bucketed",
                reservation.bucketNo() == null ? "false" : "true");
        return released;
    }

    private InventoryReservation reserveOnce(String requestId, long skuId, int quantity) {
        InventoryItem item = requireItem(skuId);
        if (item.mode() == InventoryMode.BUCKETED) {
            return reserveBucket(requestId, skuId, quantity);
        }
        if (item.available() < quantity || !inventoryRepository.reserveItem(skuId, quantity)) {
            businessMetrics.increment(BusinessMetricNames.INVENTORY_REJECTED, "reason", "insufficient_stock");
            return inventoryRepository
                    .saveReservation(InventoryReservation.rejected(requestId, skuId, quantity, "INSUFFICIENT_STOCK"));
        }
        InventoryReservation reservation = inventoryRepository.saveReservation(InventoryReservation.reserved(requestId,
                skuId, quantity, null, Instant.now().plus(Duration.ofMinutes(15))));
        appendStockLedger("reserve:" + requestId, skuId, requestId, InventoryLedgerOperation.STOCK_RESERVED, null, 0,
                quantity, 0);
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
                appendStockLedger("reserve:" + requestId, skuId, requestId, InventoryLedgerOperation.STOCK_RESERVED,
                        bucket.bucketNo(), 0, quantity, 0);
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

    public InventoryReservation reservation(String requestId) {
        var route = shardRouteIndex.resolve("inventory-reservation", requestId);
        if (route.isPresent()) {
            return shardRoutingOperations.executeRead("inventory_item", route.getAsLong(),
                    () -> inventoryRepository.findReservation(requestId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reservation not found")));
        }
        if (shardRoutingOperations.physicalShardCount("inventory_item") > 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "required inventory shard route does not exist");
        }
        return inventoryRepository.findReservation(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reservation not found"));
    }

    private InventoryItem getInShard(long skuId) {
        InventoryItem item = requireItem(skuId);
        if (item.mode() != InventoryMode.BUCKETED) {
            return item;
        }
        var summary = inventoryRepository.summarizeBuckets(skuId);
        if (summary.bucketCount() == 0) {
            throw new IllegalStateException("bucketed inventory has no buckets for skuId=" + skuId);
        }
        return item.aggregate(summary);
    }

    private InventoryItem requireItem(long skuId) {
        return inventoryRepository.findItem(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "inventory item not found"));
    }

    private void appendEvent(InventoryReservation reservation, String eventType) {
        outboxRepository.save(OutboxEvent.create("inventory-event-" + idGenerator.nextId(), "InventoryReservation",
                reservation.requestId(), eventType, "inventory", "0.1.0",
                new InventoryReservationEventPayload(reservation.requestId(), reservation.skuId(),
                        reservation.quantity(), reservation.bucketNo(), reservation.status().name())));
    }

    private void appendStockLedger(String ledgerId, long skuId, String requestId, InventoryLedgerOperation operation,
            Integer bucketNo, long totalDelta, long reservedDelta, long soldDelta) {
        InventoryStockLedger ledger = new InventoryStockLedger(ledgerId, skuId, requestId, operation, bucketNo,
                totalDelta, reservedDelta, soldDelta, Instant.now());
        if (!inventoryRepository.appendStockLedger(ledger)) {
            throw new BusinessException(ErrorCode.CONFLICT, "duplicate inventory stock ledger: " + ledgerId);
        }
    }

    private static IdempotencyService localIdempotencyService() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                Duration.ofDays(1));
    }
}
