package com.emall.inventory.repository;

import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryReservation;
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
    public InventoryReservation saveReservation(InventoryReservation reservation) {
        reservations.put(reservation.requestId(), reservation);
        return reservation;
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

    private String bucketKey(long skuId, int bucketNo) {
        return skuId + ":" + bucketNo;
    }
}
