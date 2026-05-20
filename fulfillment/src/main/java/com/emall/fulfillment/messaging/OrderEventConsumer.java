package com.emall.fulfillment.messaging;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.fulfillment.repository.ProcessedMessageRepository;
import com.emall.fulfillment.service.FulfillmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderEventConsumer {
    private final FulfillmentService fulfillmentService;
    private final MessageConsumerTemplate consumerTemplate;
    private final String defaultWarehouseCode;

    public OrderEventConsumer(ObjectMapper objectMapper, FulfillmentService fulfillmentService,
            ProcessedMessageRepository processedMessageRepository, BusinessMetrics businessMetrics,
            @Value("${emall.events.order-consumer-max-attempts:4}") int maxAttempts,
            @Value("${emall.fulfillment.default-warehouse-code}") String defaultWarehouseCode) {
        this.fulfillmentService = fulfillmentService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "fulfillment-order-consumer");
        this.defaultWarehouseCode = defaultWarehouseCode;
    }

    @Transactional
    @KafkaListener(topics = "${emall.events.order-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(String message) throws JsonProcessingException {
        consumerTemplate.consume(message, EventTypes.ORDER_PAID, this::allocate);
    }

    private void allocate(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        fulfillmentService.allocate(longValue(payload.get("orderId")), longValue(payload.get("userId")),
                longValue(payload.get("skuId")), intValue(payload.get("quantity")), defaultWarehouseCode);
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
