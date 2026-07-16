package com.emall.inventory.job;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.inventory.repository.InventoryRepository;
import com.emall.inventory.service.InventoryService;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationJob {
    private static final String TASK_NAME = "inventory.reservation.release-expired";

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final PartitionedShardWorkCoordinator workCoordinator;
    private final int maxPartitionsPerRun;
    private final Duration leaseTtl;

    public ReservationExpirationJob(InventoryRepository inventoryRepository, InventoryService inventoryService,
            PartitionedShardWorkCoordinator workCoordinator,
            @Value("${emall.jobs.reservation-expiration.max-partitions-per-run:8}") int maxPartitionsPerRun,
            @Value("${emall.jobs.reservation-expiration.lease-ttl:30s}") Duration leaseTtl) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.workCoordinator = workCoordinator;
        this.maxPartitionsPerRun = maxPartitionsPerRun;
        this.leaseTtl = leaseTtl;
    }

    @Scheduled(fixedDelay = 5000)
    public void releaseExpiredReservations() {
        releaseExpiredReservations(100);
    }

    public int releaseExpiredReservations(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int perPartitionLimit = Math.max(1, (boundedLimit + maxPartitionsPerRun - 1) / maxPartitionsPerRun);
        AtomicInteger remaining = new AtomicInteger(boundedLimit);
        Instant expiredBefore = Instant.now();
        return workCoordinator.execute(TASK_NAME, "inventory_reservation", maxPartitionsPerRun, leaseTtl, lease -> {
            int requested = Math.min(perPartitionLimit, remaining.get());
            if (requested <= 0) {
                return 0;
            }
            int processed = 0;
            for (var reservation : inventoryRepository.findExpiredReservations(expiredBefore, requested)) {
                lease.requireValid();
                if (remaining.get() <= 0) {
                    break;
                }
                remaining.decrementAndGet();
                inventoryService.release(reservation.requestId());
                processed++;
            }
            return processed;
        });
    }
}
