package com.emall.analytics;

import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CoreBusinessEventConsumer {
    private final ObjectMapper objectMapper;
    private final AnalyticsService analyticsService;
    private final MessageConsumerTemplate consumerTemplate;

    CoreBusinessEventConsumer(ObjectMapper objectMapper, AnalyticsService analyticsService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.core-consumer-max-attempts:4}") int maxAttempts) {
        this.objectMapper = objectMapper;
        this.analyticsService = analyticsService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "analytics-core-event-consumer");
    }

    @Transactional
    @KafkaListener(
            topics = {"${emall.events.order-topic:emall.order.events}",
                    "${emall.events.payment-topic:emall.payment.events}",
                    "${emall.events.inventory-topic:emall.inventory.events}"},
            groupId = "${spring.kafka.consumer.group-id:analytics}")
    void onCoreEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        consumerTemplate.consume(message, event.eventType(), analyticsService::recordBusinessEvent);
    }
}
