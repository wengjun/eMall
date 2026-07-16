package com.emall.inventory.messaging;

import com.emall.common.event.EventContractRegistry;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.inventory.service.InventoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class OrderFailureEventConsumer {
    private final InventoryService inventoryService;
    private final MessageConsumerTemplate consumerTemplate;
    private final ObjectMapper objectMapper;
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;

    public OrderFailureEventConsumer(ObjectMapper objectMapper, InventoryService inventoryService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.order-failure-consumer-max-attempts:4}") int maxAttempts) {
        this(objectMapper, inventoryService, businessMetrics, processedMessageRepository, maxAttempts, null,
                ShardRoutingOperations.noop(), ShardRouteIndex.local(), new InMemoryAggregateVersionGuard());
    }

    @Autowired
    public OrderFailureEventConsumer(ObjectMapper objectMapper, InventoryService inventoryService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.order-failure-consumer-max-attempts:4}") int maxAttempts,
            PlatformTransactionManager transactionManager, ShardRoutingOperations shardRoutingOperations,
            ShardRouteIndex shardRouteIndex, AggregateVersionGuard aggregateVersionGuard) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.shardRoutingOperations = shardRoutingOperations;
        this.shardRouteIndex = shardRouteIndex;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "inventory-order-failure-consumer", transactionManager, aggregateVersionGuard);
    }

    @KafkaListener(topics = "${emall.events.order-topic:emall.order.events}",
            groupId = "${spring.kafka.consumer.group-id:inventory}")
    public void onOrderEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!EventTypes.ORDER_CANCELLED.equals(event.eventType())) {
            consumerTemplate.consume(event, EventTypes.ORDER_CANCELLED, this::releaseReservation);
            return;
        }
        EventContractRegistry.validate(event);
        String reservationId = OrderEventPayload.from(event).inventoryReservationId();
        if (reservationId.isBlank()) {
            consumerTemplate.consume(event, EventTypes.ORDER_CANCELLED, this::releaseReservation);
            return;
        }
        long skuId = shardRouteIndex.resolveRequired("inventory-reservation", reservationId, reservationId.hashCode());
        shardRoutingOperations.execute("processed_message", skuId,
                () -> consumerTemplate.consume(event, EventTypes.ORDER_CANCELLED, this::releaseReservation));
    }

    private void releaseReservation(OutboxEvent event) {
        String reservationId = OrderEventPayload.from(event).inventoryReservationId();
        if (!reservationId.isBlank()) {
            inventoryService.release(reservationId);
        }
    }
}
