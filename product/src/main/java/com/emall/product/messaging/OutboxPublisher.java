package com.emall.product.messaging;

import com.emall.common.outbox.OutboxPublisherSupport;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.task.DistributedTaskLock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher extends OutboxPublisherSupport {
    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, @Value("${emall.events.product-topic}") String topic,
            DistributedTaskLock taskLock, BusinessMetrics businessMetrics) {
        super(outboxRepository, kafkaTemplate, objectMapper, "product", topic, taskLock, businessMetrics);
    }

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        publishScheduledBatch();
    }
}
