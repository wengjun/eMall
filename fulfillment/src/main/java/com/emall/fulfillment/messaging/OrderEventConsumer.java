package com.emall.fulfillment.messaging;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.fulfillment.repository.ProcessedMessageRepository;
import com.emall.fulfillment.service.FulfillmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class OrderEventConsumer {
    private final FulfillmentService fulfillmentService;
    private final MessageConsumerTemplate consumerTemplate;
    private final String defaultWarehouseCode;

    public OrderEventConsumer(ObjectMapper objectMapper, FulfillmentService fulfillmentService,
            ProcessedMessageRepository processedMessageRepository, BusinessMetrics businessMetrics,
            @Value("${emall.events.order-consumer-max-attempts:4}") int maxAttempts,
            @Value("${emall.fulfillment.default-warehouse-code}") String defaultWarehouseCode) {
        this(objectMapper, fulfillmentService, processedMessageRepository, businessMetrics, maxAttempts,
                defaultWarehouseCode, null, new InMemoryAggregateVersionGuard());
    }

    @Autowired
    public OrderEventConsumer(ObjectMapper objectMapper, FulfillmentService fulfillmentService,
            ProcessedMessageRepository processedMessageRepository, BusinessMetrics businessMetrics,
            @Value("${emall.events.order-consumer-max-attempts:4}") int maxAttempts,
            @Value("${emall.fulfillment.default-warehouse-code}") String defaultWarehouseCode,
            PlatformTransactionManager transactionManager, AggregateVersionGuard aggregateVersionGuard) {
        this.fulfillmentService = fulfillmentService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "fulfillment-order-consumer", transactionManager, aggregateVersionGuard);
        this.defaultWarehouseCode = defaultWarehouseCode;
    }

    @KafkaListener(topics = "${emall.events.order-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(String message) throws JsonProcessingException {
        consumerTemplate.consume(message, EventTypes.ORDER_PAID, this::allocate);
    }

    private void allocate(OutboxEvent event) {
        OrderEventPayload payload = OrderEventPayload.from(event);
        fulfillmentService.allocate(payload.orderId(), payload.userId(), payload.skuId(), payload.quantity(),
                defaultWarehouseCode);
    }
}
