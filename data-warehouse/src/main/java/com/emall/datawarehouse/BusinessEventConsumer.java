package com.emall.datawarehouse;

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
class BusinessEventConsumer {
    private final ObjectMapper objectMapper;
    private final DataWarehouseService dataWarehouseService;
    private final MessageConsumerTemplate consumerTemplate;

    BusinessEventConsumer(ObjectMapper objectMapper, DataWarehouseService dataWarehouseService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.business-consumer-max-attempts:4}") int maxAttempts) {
        this.objectMapper = objectMapper;
        this.dataWarehouseService = dataWarehouseService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "data-warehouse-business-event-consumer");
    }

    @Transactional
    @KafkaListener(
            topics = {"${emall.events.order-topic:emall.order.events}",
                    "${emall.events.payment-topic:emall.payment.events}",
                    "${emall.events.inventory-topic:emall.inventory.events}",
                    "${emall.events.product-topic:emall.product.events}"},
            groupId = "${spring.kafka.consumer.group-id:data-warehouse}")
    void onBusinessEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        consumerTemplate.consume(message, event.eventType(), dataWarehouseService::recordBusinessEvent);
    }
}
