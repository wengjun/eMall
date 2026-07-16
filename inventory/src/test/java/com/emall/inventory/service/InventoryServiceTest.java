package com.emall.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.OutboxEvent;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryLedgerOperation;
import com.emall.inventory.domain.InventoryMode;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InMemoryInventoryRepository;
import com.emall.inventory.repository.InMemoryOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
    private final InventoryService inventoryService =
            new InventoryService(inventoryRepository, outboxRepository, new SnowflakeIdGenerator(2));

    @Test
    void shouldReserveAndConfirmBucketedInventory() {
        inventoryService.addStock("stock-001", 30001L, 20);
        inventoryService.initializeBuckets(30001L, 4);

        InventoryReservation reserved = inventoryService.reserve("reserve-001", 30001L, 3);
        InventoryReservation confirmed = inventoryService.confirm("reserve-001");

        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reserved.bucketNo()).isNotNull();
        assertThat(confirmed.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(drainOutbox()).hasSize(2).extracting(OutboxEvent::aggregateVersion).containsExactly(1L, 2L);
    }

    @Test
    void shouldRejectWhenStockIsInsufficient() {
        inventoryService.addStock("stock-002", 30001L, 2);

        InventoryReservation reservation = inventoryService.reserve("reserve-002", 30001L, 3);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.reason()).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void shouldNotDuplicateStockWhenBucketsAreInitializedAgain() {
        inventoryService.addStock("stock-003", 30002L, 20);

        inventoryService.initializeBuckets(30002L, 4);
        inventoryService.initializeBuckets(30002L, 6);

        long bucketedTotal = inventoryService.buckets(30002L).stream().mapToLong(bucket -> bucket.total()).sum();
        assertThat(bucketedTotal).isEqualTo(20);
        assertThat(inventoryService.buckets(30002L)).hasSize(6);
    }

    @Test
    void shouldReleaseBucketedReservationAndRestoreAvailableStock() {
        inventoryService.addStock("stock-004", 30004L, 20);
        inventoryService.initializeBuckets(30004L, 4);
        InventoryReservation reserved = inventoryService.reserve("reserve-004", 30004L, 3);

        InventoryReservation released = inventoryService.release("reserve-004");

        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventoryService.buckets(30004L).stream().mapToLong(bucket -> bucket.reserved()).sum()).isZero();
        assertThat(inventoryService.buckets(30004L).stream().mapToLong(bucket -> bucket.available()).sum())
                .isEqualTo(20);
        assertThat(drainOutbox()).extracting(OutboxEvent::eventType).contains("inventory.reserved",
                "inventory.released");
    }

    @Test
    void shouldRejectSameRequestIdWithDifferentReservePayload() {
        inventoryService.addStock("stock-005", 30003L, 20);
        inventoryService.reserve("reserve-003", 30003L, 1);

        assertThatThrownBy(() -> inventoryService.reserve("reserve-003", 30003L, 2))
                .hasMessageContaining("idempotency key already used");
    }

    @Test
    void shouldKeepBucketedStockAsOneAggregatedLedger() {
        long skuId = 30005L;
        inventoryService.addStock("stock-ledger-1", skuId, 20);
        inventoryService.initializeBuckets(skuId, 4);
        inventoryService.reserve("reserve-ledger-1", skuId, 3);
        inventoryService.confirm("reserve-ledger-1");

        InventoryItem afterRestock = inventoryService.addStock("stock-ledger-2", skuId, 5);

        assertThat(afterRestock.mode()).isEqualTo(InventoryMode.BUCKETED);
        assertThat(afterRestock.total()).isEqualTo(25);
        assertThat(afterRestock.reserved()).isZero();
        assertThat(afterRestock.sold()).isEqualTo(3);
        assertThat(afterRestock.available()).isEqualTo(22);
        assertThat(inventoryService.buckets(skuId).stream().mapToLong(bucket -> bucket.total()).sum()).isEqualTo(25);
        assertThat(inventoryRepository.findStockLedger(skuId, 20)).extracting(entry -> entry.operation())
                .containsExactlyInAnyOrder(InventoryLedgerOperation.STOCK_ADDED,
                        InventoryLedgerOperation.BUCKETS_ACTIVATED, InventoryLedgerOperation.STOCK_RESERVED,
                        InventoryLedgerOperation.STOCK_CONFIRMED, InventoryLedgerOperation.STOCK_ADDED);
    }

    @Test
    void shouldPreserveSingleRowReservationWhenBucketsAreActivated() {
        long skuId = 30006L;
        inventoryService.addStock("stock-transition", skuId, 20);
        InventoryReservation reservation = inventoryService.reserve("reserve-before-buckets", skuId, 3);

        inventoryService.initializeBuckets(skuId, 4);
        inventoryService.confirm(reservation.requestId());

        InventoryItem item = inventoryService.get(skuId);
        assertThat(item.mode()).isEqualTo(InventoryMode.BUCKETED);
        assertThat(item.total()).isEqualTo(20);
        assertThat(item.reserved()).isZero();
        assertThat(item.sold()).isEqualTo(3);
        assertThat(item.available()).isEqualTo(17);
    }

    @Test
    void shouldApplyStockRequestOnlyOnce() {
        long skuId = 30007L;

        inventoryService.addStock("stock-idempotent", skuId, 7);
        InventoryItem replayed = inventoryService.addStock("stock-idempotent", skuId, 7);

        assertThat(replayed.total()).isEqualTo(7);
        assertThat(inventoryService.get(skuId).total()).isEqualTo(7);
        assertThat(inventoryRepository.findStockLedger(skuId, 10)).hasSize(1);
    }

    @Test
    void shouldRejectStockRequestIdReusedWithDifferentQuantity() {
        long skuId = 30008L;
        inventoryService.addStock("stock-conflict", skuId, 7);

        assertThatThrownBy(() -> inventoryService.addStock("stock-conflict", skuId, 8))
                .hasMessageContaining("idempotency key already used");
    }

    private List<OutboxEvent> drainOutbox() {
        List<OutboxEvent> drained = new ArrayList<>();
        Instant now = Instant.now().plusSeconds(1);
        while (true) {
            List<OutboxEvent> claimed =
                    outboxRepository.claimPublishable("inventory-test", now, Duration.ofSeconds(30), 100);
            if (claimed.isEmpty()) {
                return List.copyOf(drained);
            }
            claimed.forEach(event -> {
                drained.add(event);
                outboxRepository.save(event.published());
            });
        }
    }
}
