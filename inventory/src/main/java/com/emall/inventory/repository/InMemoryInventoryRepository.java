package com.emall.inventory.repository;

import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryMode;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.InventoryStockSummary;
import com.emall.inventory.domain.InventoryStockLedger;
import com.emall.inventory.domain.ReservationStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryInventoryRepository implements InventoryRepository {
    private final ConcurrentMap<Long, InventoryItem> items = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, InventoryBucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, InventoryReservation> reservations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, InventoryStockLedger> stockLedger = new ConcurrentHashMap<>();

    public InMemoryInventoryRepository() {
        saveItem(new InventoryItem(10001L, 1_000_000, 0, 0, Instant.now()));
        saveItem(new InventoryItem(10002L, 1_000_000, 0, 0, Instant.now()));
    }

    @Override
    public InventoryItem saveItem(InventoryItem item) {
        items.put(item.skuId(), item);
        return item;
    }

    @Override
    public Optional<InventoryItem> findItem(long skuId) {
        return Optional.ofNullable(items.get(skuId));
    }

    @Override
    public Optional<InventoryItem> findItemForUpdate(long skuId) {
        return findItem(skuId);
    }

    @Override
    public InventoryItem ensureItem(long skuId) {
        return items.computeIfAbsent(skuId, key -> new InventoryItem(key, 0, 0, 0, Instant.now()));
    }

    @Override
    public boolean addItemStock(long skuId, int quantity, InventoryMode expectedMode) {
        AtomicFlag updated = new AtomicFlag();
        items.computeIfPresent(skuId, (key, item) -> {
            if (item.mode() != expectedMode) {
                return item;
            }
            updated.mark();
            return item.add(quantity);
        });
        return updated.value();
    }

    @Override
    public synchronized boolean initializeBuckets(InventoryItem expectedItem, List<InventoryBucket> initialBuckets) {
        InventoryItem current = items.get(expectedItem.skuId());
        if (current == null || current.mode() != InventoryMode.SINGLE_ROW
                || current.version() != expectedItem.version()) {
            return false;
        }
        initialBuckets.forEach(bucket -> buckets.put(bucketKey(bucket.skuId(), bucket.bucketNo()), bucket));
        items.put(current.skuId(), current.activateBuckets());
        return true;
    }

    @Override
    public boolean createBucketIfAbsent(InventoryBucket bucket) {
        return buckets.putIfAbsent(bucketKey(bucket.skuId(), bucket.bucketNo()), bucket) == null;
    }

    @Override
    public boolean addBucketStock(long skuId, int bucketNo, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        buckets.computeIfPresent(bucketKey(skuId, bucketNo), (key, bucket) -> {
            updated.mark();
            return bucket.add(quantity);
        });
        return updated.value();
    }

    @Override
    public InventoryStockSummary summarizeBuckets(long skuId) {
        List<InventoryBucket> skuBuckets = findBuckets(skuId);
        Instant updatedAt =
                skuBuckets.stream().map(InventoryBucket::updatedAt).max(Comparator.naturalOrder()).orElse(null);
        return new InventoryStockSummary(skuBuckets.stream().mapToLong(InventoryBucket::total).sum(),
                skuBuckets.stream().mapToLong(InventoryBucket::reserved).sum(),
                skuBuckets.stream().mapToLong(InventoryBucket::sold).sum(), skuBuckets.size(), updatedAt);
    }

    @Override
    public InventoryBucket saveBucket(InventoryBucket bucket) {
        buckets.put(bucketKey(bucket.skuId(), bucket.bucketNo()), bucket);
        return bucket;
    }

    @Override
    public List<InventoryBucket> findBuckets(long skuId) {
        return buckets.values().stream().filter(bucket -> bucket.skuId() == skuId)
                .sorted(Comparator.comparing(InventoryBucket::bucketNo)).toList();
    }

    @Override
    public Optional<InventoryBucket> findBucket(long skuId, int bucketNo) {
        return Optional.ofNullable(buckets.get(bucketKey(skuId, bucketNo)));
    }

    @Override
    public Optional<InventoryBucket> findReservableBucket(long skuId, int quantity) {
        return findBuckets(skuId).stream().filter(bucket -> bucket.available() >= quantity)
                .min(Comparator.comparing(InventoryBucket::reserved));
    }

    @Override
    public boolean reserveItem(long skuId, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        items.computeIfPresent(skuId, (key, item) -> {
            if (item.mode() != InventoryMode.SINGLE_ROW || item.available() < quantity) {
                return item;
            }
            updated.mark();
            return item.reserve(quantity);
        });
        return updated.value();
    }

    @Override
    public boolean confirmItem(long skuId, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        items.computeIfPresent(skuId, (key, item) -> {
            if (item.reserved() < quantity) {
                return item;
            }
            updated.mark();
            return item.confirm(quantity);
        });
        return updated.value();
    }

    @Override
    public boolean releaseItem(long skuId, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        items.computeIfPresent(skuId, (key, item) -> {
            if (item.reserved() < quantity) {
                return item;
            }
            updated.mark();
            return item.release(quantity);
        });
        return updated.value();
    }

    @Override
    public boolean reserveBucket(long skuId, int bucketNo, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        buckets.computeIfPresent(bucketKey(skuId, bucketNo), (key, bucket) -> {
            if (bucket.available() < quantity) {
                return bucket;
            }
            updated.mark();
            return bucket.reserve(quantity);
        });
        return updated.value();
    }

    @Override
    public boolean confirmBucket(long skuId, int bucketNo, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        buckets.computeIfPresent(bucketKey(skuId, bucketNo), (key, bucket) -> {
            if (bucket.reserved() < quantity) {
                return bucket;
            }
            updated.mark();
            return bucket.confirm(quantity);
        });
        return updated.value();
    }

    @Override
    public boolean releaseBucket(long skuId, int bucketNo, int quantity) {
        AtomicFlag updated = new AtomicFlag();
        buckets.computeIfPresent(bucketKey(skuId, bucketNo), (key, bucket) -> {
            if (bucket.reserved() < quantity) {
                return bucket;
            }
            updated.mark();
            return bucket.release(quantity);
        });
        return updated.value();
    }

    @Override
    public InventoryReservation saveReservation(InventoryReservation reservation) {
        reservations.put(reservation.requestId(), reservation);
        return reservation;
    }

    @Override
    public boolean updateReservationStatus(String requestId, ReservationStatus expectedStatus,
            InventoryReservation reservation) {
        AtomicFlag updated = new AtomicFlag();
        reservations.computeIfPresent(requestId, (key, existing) -> {
            if (existing.status() != expectedStatus) {
                return existing;
            }
            updated.mark();
            return reservation;
        });
        return updated.value();
    }

    @Override
    public Optional<InventoryReservation> findReservation(String requestId) {
        return Optional.ofNullable(reservations.get(requestId));
    }

    @Override
    public List<InventoryReservation> findExpiredReservations(Instant now, int limit) {
        return reservations.values().stream().filter(reservation -> reservation.status() == ReservationStatus.RESERVED)
                .filter(reservation -> !reservation.expiresAt().isAfter(now))
                .sorted(Comparator.comparing(InventoryReservation::expiresAt)).limit(limit).toList();
    }

    @Override
    public boolean appendStockLedger(InventoryStockLedger ledger) {
        return stockLedger.putIfAbsent(ledger.ledgerId(), ledger) == null;
    }

    @Override
    public List<InventoryStockLedger> findStockLedger(long skuId, int limit) {
        return stockLedger
                .values().stream().filter(entry -> entry.skuId() == skuId).sorted(Comparator
                        .comparing(InventoryStockLedger::createdAt).thenComparing(InventoryStockLedger::ledgerId))
                .limit(limit).toList();
    }

    private String bucketKey(long skuId, int bucketNo) {
        return skuId + ":" + bucketNo;
    }

    private static final class AtomicFlag {
        private boolean value;

        void mark() {
            value = true;
        }

        boolean value() {
            return value;
        }
    }
}
