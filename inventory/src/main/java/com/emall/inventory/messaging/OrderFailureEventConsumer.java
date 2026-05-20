package com.emall.inventory.messaging;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.inventory.service.InventoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderFailureEventConsumer {
    private final InventoryService inventoryService;
    private final MessageConsumerTemplate consumerTemplate;

    public OrderFailureEventConsumer(ObjectMapper objectMapper, InventoryService inventoryService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.order-failure-consumer-max-attempts:4}") int maxAttempts) {
        this.inventoryService = inventoryService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "inventory-order-failure-consumer");
    }

    @Transactional
    @KafkaListener(topics = "${emall.events.order-topic:emall.order.events}",
            groupId = "${spring.kafka.consumer.group-id:inventory}")
    public void onOrderEvent(String message) throws JsonProcessingException {
        consumerTemplate.consume(message, EventTypes.ORDER_CANCELLED, this::releaseReservation);
    }

    private void releaseReservation(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        String reservationId = String.valueOf(payload.get("inventoryReservationId"));
        if (!reservationId.isBlank() && !"null".equals(reservationId)) {
            inventoryService.release(reservationId);
        }
    }
}
