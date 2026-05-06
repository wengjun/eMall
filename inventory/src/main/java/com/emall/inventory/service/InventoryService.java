package com.emall.inventory.service;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InventoryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;

    public InventoryService(InventoryRepository inventoryRepository, OutboxRepository outboxRepository,
                            SnowflakeIdGenerator idGenerator) {
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
    }

    public InventoryItem get(long skuId) {
        return inventoryRepository.findItem(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "inventory item not found"));
    }

    @Transactional
    public synchronized InventoryItem addStock(long skuId, int quantity) {
        InventoryItem item = inventoryRepository.findItem(skuId)
                .orElse(new InventoryItem(skuId, 0, 0, 0, Instant.now()));
        return inventoryRepository.saveItem(item.add(quantity));
    }

    @Transactional
    public synchronized List<InventoryBucket> initializeBuckets(long skuId, int bucketCount) {
        if (bucketCount < 2 || bucketCount > 256) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "bucket count must be between 2 and 256");
        }
        InventoryItem item = get(skuId);
        int baseQuantity = item.available() / bucketCount;
        int remainder = item.available() % bucketCount;
        for (int bucketNo = 0; bucketNo < bucketCount; bucketNo++) {
            int quantity = baseQuantity + (bucketNo < remainder ? 1 : 0);
            InventoryBucket bucket = inventoryRepository.findBucket(skuId, bucketNo)
                    .orElse(new InventoryBucket(skuId, bucketNo, 0, 0, 0, Instant.now()));
            inventoryRepository.saveBucket(bucket.add(quantity));
        }
        return inventoryRepository.findBuckets(skuId);
    }

    public List<InventoryBucket> buckets(long skuId) {
        return inventoryRepository.findBuckets(skuId);
    }

    @Transactional
    public synchronized InventoryReservation reserve(String requestId, long skuId, int quantity) {
        return inventoryRepository.findReservation(requestId)
                .orElseGet(() -> reserveOnce(requestId, skuId, quantity));
    }

    @Transactional
    public synchronized InventoryReservation confirm(String requestId) {
        InventoryReservation reservation = reservation(requestId);
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            return reservation;
        }
        if (reservation.status() != ReservationStatus.RESERVED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "reservation cannot be confirmed from " + reservation.status());
        }
        if (reservation.bucketNo() == null) {
            InventoryItem item = get(reservation.skuId()).confirm(reservation.quantity());
            inventoryRepository.saveItem(item);
        } else {
            InventoryBucket bucket = bucket(reservation).confirm(reservation.quantity());
            inventoryRepository.saveBucket(bucket);
        }
        InventoryReservation confirmed = inventoryRepository.saveReservation(reservation.confirm());
        appendEvent(confirmed, EventTypes.INVENTORY_CONFIRMED);
        return confirmed;
    }

    @Transactional
    public synchronized InventoryReservation release(String requestId) {
        InventoryReservation reservation = reservation(requestId);
        if (reservation.status() == ReservationStatus.RELEASED) {
            return reservation;
        }
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CONFLICT, "confirmed reservation cannot be released");
        }
        if (reservation.status() == ReservationStatus.REJECTED) {
            return reservation;
        }
        if (reservation.bucketNo() == null) {
            InventoryItem item = get(reservation.skuId()).release(reservation.quantity());
            inventoryRepository.saveItem(item);
        } else {
            InventoryBucket bucket = bucket(reservation).release(reservation.quantity());
            inventoryRepository.saveBucket(bucket);
        }
        InventoryReservation released = inventoryRepository.saveReservation(reservation.release());
        appendEvent(released, EventTypes.INVENTORY_RELEASED);
        return released;
    }

    private InventoryReservation reserveOnce(String requestId, long skuId, int quantity) {
        List<InventoryBucket> buckets = inventoryRepository.findBuckets(skuId);
        if (!buckets.isEmpty()) {
            return reserveBucket(requestId, skuId, quantity);
        }
        InventoryItem item = get(skuId);
        if (item.available() < quantity) {
            return inventoryRepository.saveReservation(
                    InventoryReservation.rejected(requestId, skuId, quantity, "INSUFFICIENT_STOCK"));
        }
        inventoryRepository.saveItem(item.reserve(quantity));
        InventoryReservation reservation = inventoryRepository.saveReservation(
                InventoryReservation.reserved(
                        requestId, skuId, quantity, null, Instant.now().plus(Duration.ofMinutes(15))));
        appendEvent(reservation, EventTypes.INVENTORY_RESERVED);
        return reservation;
    }

    private InventoryReservation reserveBucket(String requestId, long skuId, int quantity) {
        InventoryBucket bucket = inventoryRepository.findReservableBucket(skuId, quantity).orElse(null);
        if (bucket == null) {
            return inventoryRepository.saveReservation(
                    InventoryReservation.rejected(requestId, skuId, quantity, "INSUFFICIENT_BUCKET_STOCK"));
        }
        inventoryRepository.saveBucket(bucket.reserve(quantity));
        InventoryReservation reservation = inventoryRepository.saveReservation(
                InventoryReservation.reserved(
                        requestId, skuId, quantity, bucket.bucketNo(), Instant.now().plus(Duration.ofMinutes(15))));
        appendEvent(reservation, EventTypes.INVENTORY_RESERVED);
        return reservation;
    }

    private InventoryReservation reservation(String requestId) {
        return inventoryRepository.findReservation(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reservation not found"));
    }

    private InventoryBucket bucket(InventoryReservation reservation) {
        return inventoryRepository.findBucket(reservation.skuId(), reservation.bucketNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "inventory bucket not found"));
    }

    private void appendEvent(InventoryReservation reservation, String eventType) {
        outboxRepository.save(OutboxEvent.create(
                "inventory-event-" + idGenerator.nextId(),
                "InventoryReservation",
                reservation.requestId(),
                eventType,
                Map.of(
                        "requestId", reservation.requestId(),
                        "skuId", reservation.skuId(),
                        "quantity", reservation.quantity(),
                        "bucketNo", reservation.bucketNo() == null ? "" : reservation.bucketNo(),
                        "status", reservation.status().name()
                )));
    }
}
