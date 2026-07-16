package com.emall.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import com.emall.inventory.repository.InMemoryInventoryRepository;
import com.emall.inventory.repository.InMemoryOutboxRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class InventoryConcurrencyTest {
    @Test
    void shouldNotOverReserveStockWhenRequestsRace() throws Exception {
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        InventoryService inventoryService =
                new InventoryService(inventoryRepository, new InMemoryOutboxRepository(), new SnowflakeIdGenerator(2));
        long skuId = 930001L;
        inventoryService.addStock("race-initial-stock", skuId, 20);

        ExecutorService executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<InventoryReservation>> tasks = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            String requestId = "race-reserve-" + index;
            tasks.add(() -> {
                start.await();
                return inventoryService.reserve(requestId, skuId, 1);
            });
        }

        List<Future<InventoryReservation>> futures = tasks.stream().map(executor::submit).toList();
        start.countDown();
        List<InventoryReservation> reservations = new ArrayList<>();
        for (Future<InventoryReservation> future : futures) {
            reservations.add(future.get());
        }
        executor.shutdownNow();

        long reserved =
                reservations.stream().filter(reservation -> reservation.status() == ReservationStatus.RESERVED).count();
        assertThat(reserved).isEqualTo(20);
        assertThat(inventoryService.get(skuId).reserved()).isEqualTo(20);
        assertThat(inventoryService.get(skuId).available()).isZero();
    }

    @Test
    void shouldNotLoseConcurrentBucketedRestocks() throws Exception {
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        InventoryService inventoryService =
                new InventoryService(inventoryRepository, new InMemoryOutboxRepository(), new SnowflakeIdGenerator(2));
        long skuId = 930002L;
        inventoryService.addStock("bucketed-initial-stock", skuId, 1);
        inventoryService.initializeBuckets(skuId, 8);

        ExecutorService executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            int requestNo = index;
            tasks.add(() -> {
                start.await();
                inventoryService.addStock("concurrent-stock-" + requestNo, skuId, 1);
                return null;
            });
        }

        List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
        start.countDown();
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdownNow();

        assertThat(inventoryService.get(skuId).total()).isEqualTo(101);
        assertThat(inventoryService.buckets(skuId).stream().mapToLong(bucket -> bucket.total()).sum()).isEqualTo(101);
        assertThat(inventoryRepository.findStockLedger(skuId, 200)).hasSize(102);
    }
}
