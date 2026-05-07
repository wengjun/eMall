package com.emall.common.outbox;

import com.emall.common.event.OutboxEvent;
import com.emall.common.task.DistributedTaskLock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public abstract class OutboxPublisherSupport {
    private final Logger log;
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final String topic;
    private final DistributedTaskLock taskLock;

    protected OutboxPublisherSupport(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, String serviceName, String topic, DistributedTaskLock taskLock) {
        this.log = LoggerFactory.getLogger(getClass());
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        this.topic = topic;
        this.taskLock = taskLock;
    }

    protected void publishScheduledBatch() {
        publishBatch(100);
    }

    public int publishBatch(int limit) {
        return taskLock.executeIfAcquired(serviceName + ".outbox.publish", Duration.ofSeconds(30),
                () -> publishBatchUnlocked(limit));
    }

    private int publishBatchUnlocked(int limit) {
        int published = 0;
        for (OutboxEvent event : outboxRepository.findPublishable(Instant.now(), limit)) {
            try {
                kafkaTemplate.send(topic, event.aggregateId(), serialize(event)).get();
                log.info("publish {} outbox event type={} aggregateType={} aggregateId={}", serviceName,
                        event.eventType(), event.aggregateType(), event.aggregateId());
                outboxRepository.save(event.published());
                published++;
            } catch (Exception ex) {
                Instant nextRetryAt = Instant.now().plus(Duration.ofSeconds(Math.min(60, event.retryCount() + 1L)));
                outboxRepository.save(event.failed(nextRetryAt));
            }
        }
        return published;
    }

    public int retryFailedNow(int limit) {
        outboxRepository.rescheduleFailed(Instant.now(), limit);
        return publishBatch(limit);
    }

    private String serialize(OutboxEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }
}
