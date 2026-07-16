package com.emall.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.EventTypes;
import com.emall.common.event.InventoryReservationEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.EventVersionGapException;
import com.emall.common.outbox.OutboxRepository;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryLedgerOperation;
import com.emall.inventory.domain.InventoryMode;
import com.emall.inventory.domain.InventoryStockLedger;
import com.emall.inventory.domain.InventoryStockSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"emall.storage=jdbc", "spring.flyway.enabled=true",
        "spring.cloud.nacos.discovery.enabled=false", "spring.cloud.nacos.config.enabled=false",
        "spring.kafka.listener.auto-startup=false", "dubbo.application.qos-enable=false"})
class InventoryRepositoryIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_inventory")
            .withUsername("emall").withPassword("emall").withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private AggregateVersionGuard aggregateVersionGuard;

    private ExecutorService executor;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @AfterEach
    void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional
    void preservesLegacyReservationsWhenSwitchingToBucketedInventory() {
        long skuId = 9_001L;
        InventoryItem item = inventoryRepository.saveItem(new InventoryItem(skuId, 100, 0, 0, Instant.now()));
        assertThat(inventoryRepository.reserveItem(skuId, 20)).isTrue();

        item = inventoryRepository.findItemForUpdate(skuId).orElseThrow();
        List<InventoryBucket> buckets = List.of(new InventoryBucket(skuId, 0, 40, 0, 0, Instant.now()),
                new InventoryBucket(skuId, 1, 40, 0, 0, Instant.now()));
        assertThat(inventoryRepository.initializeBuckets(item, buckets)).isTrue();
        assertThat(inventoryRepository.addBucketStock(skuId, 1, 25)).isTrue();

        InventoryStockSummary summary = inventoryRepository.summarizeBuckets(skuId);
        InventoryItem aggregate = inventoryRepository.findItem(skuId).orElseThrow().aggregate(summary);
        assertThat(aggregate.mode()).isEqualTo(InventoryMode.BUCKETED);
        assertThat(aggregate.total()).isEqualTo(125);
        assertThat(aggregate.reserved()).isEqualTo(20);
        assertThat(aggregate.available()).isEqualTo(105);

        assertThat(inventoryRepository.confirmItem(skuId, 20)).isTrue();
        aggregate = inventoryRepository.findItem(skuId).orElseThrow()
                .aggregate(inventoryRepository.summarizeBuckets(skuId));
        assertThat(aggregate.total()).isEqualTo(125);
        assertThat(aggregate.reserved()).isZero();
        assertThat(aggregate.sold()).isEqualTo(20);

        InventoryStockLedger ledger = new InventoryStockLedger("it-ledger-1", skuId, "it-request-1",
                InventoryLedgerOperation.STOCK_CONFIRMED, null, 0, -20, 20, Instant.now());
        assertThat(inventoryRepository.appendStockLedger(ledger)).isTrue();
        assertThat(inventoryRepository.appendStockLedger(ledger)).isFalse();
        assertThat(inventoryRepository.findStockLedger(skuId, 10)).singleElement().satisfies(stored -> {
            assertThat(stored.ledgerId()).isEqualTo(ledger.ledgerId());
            assertThat(stored.requestId()).isEqualTo(ledger.requestId());
            assertThat(stored.operation()).isEqualTo(ledger.operation());
            assertThat(stored.reservedDelta()).isEqualTo(-20);
            assertThat(stored.soldDelta()).isEqualTo(20);
        });
    }

    @Test
    void appliesConcurrentBucketRestocksWithoutLostUpdates() {
        long skuId = 9_002L;
        inventoryRepository.saveItem(new InventoryItem(skuId, 0, 0, 0, InventoryMode.BUCKETED, 1, Instant.now()));
        inventoryRepository.saveBucket(new InventoryBucket(skuId, 0, 0, 0, 0, Instant.now()));
        executor = Executors.newFixedThreadPool(12);

        List<CompletableFuture<Boolean>> updates =
                java.util.stream.IntStream.range(0, 100).mapToObj(ignored -> CompletableFuture
                        .supplyAsync(() -> inventoryRepository.addBucketStock(skuId, 0, 1), executor)).toList();
        CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new)).join();

        assertThat(updates).allSatisfy(update -> assertThat(update.join()).isTrue());
        assertThat(inventoryRepository.summarizeBuckets(skuId).total()).isEqualTo(100);
    }

    @Test
    void allocatesAggregateVersionsAndClaimsOnlyTheAggregateHead() {
        OutboxEvent first = outboxRepository.save(inventoryEvent("it-event-1"));
        OutboxEvent second = outboxRepository.save(inventoryEvent("it-event-2"));
        Instant now = Instant.now().plusSeconds(1);

        assertThat(first.aggregateVersion()).isOne();
        assertThat(second.aggregateVersion()).isEqualTo(2);
        OutboxEvent claimed = outboxRepository.claimPublishable("it-owner", now, Duration.ofSeconds(30), 10).stream()
                .filter(event -> "it-ordering".equals(event.aggregateId())).findFirst().orElseThrow();
        assertThat(claimed.eventId()).isEqualTo("it-event-1");
        outboxRepository.save(claimed.published());

        assertThat(outboxRepository.claimPublishable("it-owner", now.plusSeconds(1), Duration.ofSeconds(30), 10))
                .filteredOn(event -> "it-ordering".equals(event.aggregateId())).singleElement()
                .extracting(OutboxEvent::eventId).isEqualTo("it-event-2");
    }

    @Test
    void persistsConsumerAggregateVersionWithCompareAndSetSemantics() {
        OutboxEvent versionOne = inventoryEvent("it-consume-1").withAggregateVersion(1);
        OutboxEvent versionTwo = inventoryEvent("it-consume-2").withAggregateVersion(2);
        OutboxEvent versionThree = inventoryEvent("it-consume-3").withAggregateVersion(3);

        assertThat(aggregateVersionGuard.tryAdvance("inventory-it", versionOne).accepted()).isTrue();
        assertThatThrownBy(() -> aggregateVersionGuard.tryAdvance("inventory-it", versionThree))
                .isInstanceOf(EventVersionGapException.class);
        assertThat(aggregateVersionGuard.tryAdvance("inventory-it", versionTwo).accepted()).isTrue();
        assertThat(aggregateVersionGuard.tryAdvance("inventory-it", versionOne).accepted()).isFalse();
        assertThat(aggregateVersionGuard.tryAdvance("inventory-it", versionThree).accepted()).isTrue();
    }

    private OutboxEvent inventoryEvent(String eventId) {
        InventoryReservationEventPayload payload =
                new InventoryReservationEventPayload("it-ordering", 9003L, 1, 0, "RESERVED");
        return OutboxEvent.create(eventId, "InventoryReservation", "it-ordering", EventTypes.INVENTORY_RESERVED,
                "inventory", "1.0.0", payload);
    }
}
