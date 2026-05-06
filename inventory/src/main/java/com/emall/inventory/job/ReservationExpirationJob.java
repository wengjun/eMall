package com.emall.inventory.job;

import com.emall.common.task.DistributedTaskLock;
import com.emall.inventory.repository.InventoryRepository;
import com.emall.inventory.service.InventoryService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationJob {
    private static final String LOCK_NAME = "inventory.reservation.release-expired";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final DistributedTaskLock taskLock;

    public ReservationExpirationJob(InventoryRepository inventoryRepository, InventoryService inventoryService,
                                    DistributedTaskLock taskLock) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.taskLock = taskLock;
    }

    @Scheduled(fixedDelay = 5000)
    public void releaseExpiredReservations() {
        releaseExpiredReservations(100);
    }

    public int releaseExpiredReservations(int limit) {
        return taskLock.executeIfAcquired(LOCK_NAME, LOCK_TTL, () -> releaseExpiredReservationsUnlocked(limit));
    }

    private int releaseExpiredReservationsUnlocked(int limit) {
        return inventoryRepository.findExpiredReservations(Instant.now(), limit).stream()
                .map(reservation -> inventoryService.release(reservation.requestId()))
                .toList()
                .size();
    }
}
