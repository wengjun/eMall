package com.emall.inventory.repository;

import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryMode;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.InventoryStockSummary;
import com.emall.inventory.domain.InventoryStockLedger;
import com.emall.inventory.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    InventoryItem saveItem(InventoryItem item);

    Optional<InventoryItem> findItem(long skuId);

    Optional<InventoryItem> findItemForUpdate(long skuId);

    InventoryItem ensureItem(long skuId);

    boolean addItemStock(long skuId, int quantity, InventoryMode expectedMode);

    boolean initializeBuckets(InventoryItem expectedItem, List<InventoryBucket> initialBuckets);

    boolean createBucketIfAbsent(InventoryBucket bucket);

    boolean addBucketStock(long skuId, int bucketNo, int quantity);

    InventoryStockSummary summarizeBuckets(long skuId);

    InventoryBucket saveBucket(InventoryBucket bucket);

    List<InventoryBucket> findBuckets(long skuId);

    Optional<InventoryBucket> findBucket(long skuId, int bucketNo);

    Optional<InventoryBucket> findReservableBucket(long skuId, int quantity);

    boolean reserveItem(long skuId, int quantity);

    boolean confirmItem(long skuId, int quantity);

    boolean releaseItem(long skuId, int quantity);

    boolean reserveBucket(long skuId, int bucketNo, int quantity);

    boolean confirmBucket(long skuId, int bucketNo, int quantity);

    boolean releaseBucket(long skuId, int bucketNo, int quantity);

    InventoryReservation saveReservation(InventoryReservation reservation);

    boolean updateReservationStatus(String requestId, ReservationStatus expectedStatus,
            InventoryReservation reservation);

    Optional<InventoryReservation> findReservation(String requestId);

    List<InventoryReservation> findExpiredReservations(Instant now, int limit);

    boolean appendStockLedger(InventoryStockLedger ledger);

    List<InventoryStockLedger> findStockLedger(long skuId, int limit);
}
