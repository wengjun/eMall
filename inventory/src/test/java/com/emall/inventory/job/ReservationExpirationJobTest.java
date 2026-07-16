package com.emall.inventory.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.task.PartitionedShardWorkCoordinator.PartitionLease;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InventoryRepository;
import com.emall.inventory.service.InventoryService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationExpirationJobTest {
    @Test
    void shouldProcessOnlyTheConfiguredBatchInsidePartitionLease() {
        InventoryRepository repository = mock(InventoryRepository.class);
        InventoryService inventoryService = mock(InventoryService.class);
        PartitionedShardWorkCoordinator workCoordinator = mock(PartitionedShardWorkCoordinator.class);
        PartitionLease lease = mock(PartitionLease.class);
        InventoryReservation first = reservation("request-1", 1L);
        InventoryReservation second = reservation("request-2", 2L);
        when(repository.findExpiredReservations(any(), eq(1))).thenReturn(List.of(first, second));
        when(workCoordinator.execute(eq("inventory.reservation.release-expired"), eq("inventory_reservation"), eq(8),
                eq(Duration.ofSeconds(30)), any()))
                .thenAnswer(invocation -> invocation
                        .getArgument(4, PartitionedShardWorkCoordinator.ShardPartitionTask.class).run(lease));
        ReservationExpirationJob job =
                new ReservationExpirationJob(repository, inventoryService, workCoordinator, 8, Duration.ofSeconds(30));

        assertThat(job.releaseExpiredReservations(1)).isEqualTo(1);
        verify(lease, atLeastOnce()).requireValid();
        verify(inventoryService).release("request-1");
        verify(inventoryService, never()).release("request-2");
    }

    private InventoryReservation reservation(String requestId, long skuId) {
        Instant now = Instant.now();
        return new InventoryReservation(requestId, skuId, 1, null, ReservationStatus.RESERVED, null,
                now.minusSeconds(60), now.minusSeconds(120), now);
    }
}
