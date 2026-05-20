package com.emall.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InMemoryInventoryRepository;
import com.emall.inventory.repository.InMemoryOutboxRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final InventoryService inventoryService =
            new InventoryService(new InMemoryInventoryRepository(), outboxRepository, new SnowflakeIdGenerator(2));

    @Test
    void shouldReserveAndConfirmBucketedInventory() {
        inventoryService.addStock(30001L, 20);
        inventoryService.initializeBuckets(30001L, 4);

        InventoryReservation reserved = inventoryService.reserve("reserve-001", 30001L, 3);
        InventoryReservation confirmed = inventoryService.confirm("reserve-001");

        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reserved.bucketNo()).isNotNull();
        assertThat(confirmed.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(outboxRepository.findPublishable(Instant.now(), 10)).hasSize(2);
    }

    @Test
    void shouldRejectWhenStockIsInsufficient() {
        inventoryService.addStock(30001L, 2);

        InventoryReservation reservation = inventoryService.reserve("reserve-002", 30001L, 3);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.reason()).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void shouldNotDuplicateStockWhenBucketsAreInitializedAgain() {
        inventoryService.addStock(30002L, 20);

        inventoryService.initializeBuckets(30002L, 4);
        inventoryService.initializeBuckets(30002L, 6);

        long bucketedTotal = inventoryService.buckets(30002L).stream().mapToLong(bucket -> bucket.total()).sum();
        assertThat(bucketedTotal).isEqualTo(20);
        assertThat(inventoryService.buckets(30002L)).hasSize(6);
    }

    @Test
    void shouldReleaseBucketedReservationAndRestoreAvailableStock() {
        inventoryService.addStock(30004L, 20);
        inventoryService.initializeBuckets(30004L, 4);
        InventoryReservation reserved = inventoryService.reserve("reserve-004", 30004L, 3);

        InventoryReservation released = inventoryService.release("reserve-004");

        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventoryService.buckets(30004L).stream().mapToLong(bucket -> bucket.reserved()).sum()).isZero();
        assertThat(inventoryService.buckets(30004L).stream().mapToLong(bucket -> bucket.available()).sum())
                .isEqualTo(20);
        assertThat(outboxRepository.findPublishable(Instant.now(), 10)).extracting(event -> event.eventType())
                .contains("inventory.reserved", "inventory.released");
    }

    @Test
    void shouldRejectSameRequestIdWithDifferentReservePayload() {
        inventoryService.addStock(30003L, 20);
        inventoryService.reserve("reserve-003", 30003L, 1);

        assertThatThrownBy(() -> inventoryService.reserve("reserve-003", 30003L, 2))
                .hasMessageContaining("idempotency key already used");
    }
}
