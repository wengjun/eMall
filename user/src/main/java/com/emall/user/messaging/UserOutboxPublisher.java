package com.emall.user.messaging;

import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.outbox.OutboxPublisherSupport;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.task.DistributedTaskLock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserOutboxPublisher extends OutboxPublisherSupport {
    public UserOutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, @Value("${emall.events.user-profile-topic}") String topic,
            DistributedTaskLock taskLock, BusinessMetrics businessMetrics,
            ShardRoutingOperations shardRoutingOperations) {
        super(outboxRepository, kafkaTemplate, objectMapper, "user", topic, taskLock, businessMetrics,
                shardRoutingOperations, "outbox_event");
    }

    @Scheduled(fixedDelayString = "${emall.events.outbox-publish-delay:1000}")
    public void publish() {
        publishScheduledBatch();
    }
}
