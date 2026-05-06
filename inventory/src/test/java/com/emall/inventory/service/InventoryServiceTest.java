package com.emall.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InMemoryInventoryRepository;
import com.emall.inventory.repository.InMemoryOutboxRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final InventoryService inventoryService = new InventoryService(
            new InMemoryInventoryRepository(),
            outboxRepository,
            new SnowflakeIdGenerator(2));

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
}
