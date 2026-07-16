package com.emall.common.outbox;

import com.emall.common.event.OutboxEvent;
import com.emall.common.event.EventContractRegistry;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.task.DistributedTaskLock;
import com.emall.common.sharding.ShardRoutingOperations;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.core.KafkaTemplate;

public abstract class OutboxPublisherSupport {
    private final Logger log;
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final String topic;
    private final String ownerId;
    private final BusinessMetrics businessMetrics;
    private final ShardRoutingOperations shardRoutingOperations;
    private final String scanLogicalTable;
    private final AtomicInteger nextShard = new AtomicInteger();
    private final Duration claimLeaseTtl = Duration.ofSeconds(30);
    private final int maxRetryAttempts = 12;

    protected OutboxPublisherSupport(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, String serviceName, String topic, DistributedTaskLock taskLock) {
        this(outboxRepository, kafkaTemplate, objectMapper, serviceName, topic, taskLock, BusinessMetrics.noop());
    }

    protected OutboxPublisherSupport(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, String serviceName, String topic, DistributedTaskLock taskLock,
            BusinessMetrics businessMetrics) {
        this(outboxRepository, kafkaTemplate, objectMapper, serviceName, topic, taskLock, businessMetrics,
                ShardRoutingOperations.noop(), "outbox_event");
    }

    protected OutboxPublisherSupport(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, String serviceName, String topic, DistributedTaskLock taskLock,
            BusinessMetrics businessMetrics, ShardRoutingOperations shardRoutingOperations, String scanLogicalTable) {
        this.log = LoggerFactory.getLogger(getClass());
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        this.topic = topic;
        this.ownerId = serviceName + "-" + UUID.randomUUID();
        this.businessMetrics = businessMetrics;
        this.shardRoutingOperations = shardRoutingOperations;
        this.scanLogicalTable = scanLogicalTable;
    }

    protected void publishScheduledBatch() {
        publishBatch(100);
    }

    public int publishBatch(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        List<RoutedOutboxEvent> events = claimAcrossShards(boundedLimit);
        businessMetrics.increment(BusinessMetricNames.OUTBOX_CLAIMED, "service", serviceName, "topic", topic);
        businessMetrics.recordGauge("emall_outbox_claimed_batch_size", events.size(), "service", serviceName);
        List<CompletableFuture<Boolean>> futures = events.stream().map(this::publishOne).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return (int) futures.stream().filter(future -> Boolean.TRUE.equals(future.join())).count();
    }

    public int retryFailedNow(int limit) {
        rescheduleFailedAcrossBoundedShards(Math.max(1, Math.min(limit, 1000)));
        return publishBatch(limit);
    }

    private int rescheduleFailedAcrossBoundedShards(int limit) {
        int shardCount = shardRoutingOperations.physicalShardCount(scanLogicalTable);
        int shardsToScan = Math.min(shardCount, 8);
        int perShardLimit = Math.max(1, (int) Math.ceil((double) limit / shardsToScan));
        int rescheduled = 0;
        for (int offset = 0; offset < shardsToScan && rescheduled < limit; offset++) {
            int shardIndex = Math.floorMod(nextShard.getAndIncrement(), shardCount);
            int remaining = limit - rescheduled;
            rescheduled += shardRoutingOperations.executePhysicalShard(scanLogicalTable, shardIndex,
                    () -> outboxRepository.rescheduleFailed(Instant.now(), Math.min(perShardLimit, remaining)));
        }
        return rescheduled;
    }

    private List<RoutedOutboxEvent> claimAcrossShards(int limit) {
        int shardCount = shardRoutingOperations.physicalShardCount(scanLogicalTable);
        int shardsToScan = Math.min(shardCount, 8);
        int perShardLimit = Math.max(1, (int) Math.ceil((double) limit / shardsToScan));
        List<RoutedOutboxEvent> claimed = new ArrayList<>(limit);
        for (int offset = 0; offset < shardsToScan && claimed.size() < limit; offset++) {
            int shardIndex = Math.floorMod(nextShard.getAndIncrement(), shardCount);
            List<OutboxEvent> events = shardRoutingOperations.executePhysicalShard(scanLogicalTable, shardIndex,
                    () -> outboxRepository.claimPublishable(ownerId, Instant.now(), claimLeaseTtl, perShardLimit));
            events.stream().limit(limit - claimed.size()).map(event -> new RoutedOutboxEvent(shardIndex, event))
                    .forEach(claimed::add);
        }
        return List.copyOf(claimed);
    }

    private CompletableFuture<Boolean> publishOne(RoutedOutboxEvent routedEvent) {
        OutboxEvent event = routedEvent.event();
        try {
            EventContractRegistry.validate(event);
            CompletableFuture<SendResult<String, String>> send =
                    kafkaTemplate.send(topic, event.aggregateId(), serialize(event));
            return send.thenApply(result -> {
                log.info("publish {} outbox event type={} aggregateType={} aggregateId={}", serviceName,
                        event.eventType(), event.aggregateType(), event.aggregateId());
                saveInOriginShard(routedEvent.shardIndex(), event.published());
                businessMetrics.increment(BusinessMetricNames.OUTBOX_PUBLISHED, "service", serviceName, "topic", topic);
                return true;
            }).exceptionally(error -> {
                saveInOriginShard(routedEvent.shardIndex(), failedEvent(event, error));
                businessMetrics.increment(BusinessMetricNames.OUTBOX_FAILED, "service", serviceName, "topic", topic);
                return false;
            });
        } catch (IllegalArgumentException ex) {
            saveInOriginShard(routedEvent.shardIndex(), event.dead("CONTRACT_INVALID", ex.getMessage()));
            businessMetrics.increment(BusinessMetricNames.OUTBOX_FAILED, "service", serviceName, "topic", topic);
            return CompletableFuture.completedFuture(false);
        } catch (JsonProcessingException ex) {
            saveInOriginShard(routedEvent.shardIndex(), failedEvent(event, ex));
            businessMetrics.increment(BusinessMetricNames.OUTBOX_FAILED, "service", serviceName, "topic", topic);
            return CompletableFuture.completedFuture(false);
        }
    }

    private OutboxEvent failedEvent(OutboxEvent event, Throwable error) {
        Throwable root = rootCause(error);
        String message = root.getClass().getSimpleName() + ": " + root.getMessage();
        if (event.retryCount() + 1 >= maxRetryAttempts) {
            return event.dead("PUBLISH_FAILED", message);
        }
        long delaySeconds = Math.min(300L, 1L << Math.min(event.retryCount() + 1, 8));
        return event.failed(Instant.now().plus(Duration.ofSeconds(delaySeconds)), "PUBLISH_FAILED", message);
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String serialize(OutboxEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }

    private void saveInOriginShard(int shardIndex, OutboxEvent event) {
        shardRoutingOperations.executePhysicalShard(scanLogicalTable, shardIndex, () -> {
            outboxRepository.save(event);
            return null;
        });
    }

    private record RoutedOutboxEvent(int shardIndex, OutboxEvent event) {
    }
}
