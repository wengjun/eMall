package com.emall.order.messaging;

import com.emall.common.event.EventContractRegistry;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.PaymentEventPayload;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.order.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class PaymentEventConsumer {
    private final OrderService orderService;
    private final MessageConsumerTemplate consumerTemplate;
    private final ObjectMapper objectMapper;
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;

    public PaymentEventConsumer(ObjectMapper objectMapper, OrderService orderService, BusinessMetrics businessMetrics,
            ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.payment-consumer-max-attempts:4}") int maxAttempts) {
        this(objectMapper, orderService, businessMetrics, processedMessageRepository, maxAttempts, null,
                ShardRoutingOperations.noop(), ShardRouteIndex.local(), new InMemoryAggregateVersionGuard());
    }

    @Autowired
    public PaymentEventConsumer(ObjectMapper objectMapper, OrderService orderService, BusinessMetrics businessMetrics,
            ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.payment-consumer-max-attempts:4}") int maxAttempts,
            PlatformTransactionManager transactionManager, ShardRoutingOperations shardRoutingOperations,
            ShardRouteIndex shardRouteIndex, AggregateVersionGuard aggregateVersionGuard) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
        this.shardRoutingOperations = shardRoutingOperations;
        this.shardRouteIndex = shardRouteIndex;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "order-payment-consumer", transactionManager, aggregateVersionGuard);
    }

    @KafkaListener(topics = "${emall.events.payment-topic:emall.payment.events}",
            groupId = "${spring.kafka.consumer.group-id:order}")
    public void onPaymentEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!EventTypes.PAYMENT_SUCCEEDED.equals(event.eventType())) {
            consumerTemplate.consume(event, EventTypes.PAYMENT_SUCCEEDED, this::markOrderPaid);
            return;
        }
        EventContractRegistry.validate(event);
        long orderId = PaymentEventPayload.from(event).orderId();
        long userId = shardRouteIndex.resolveRequired("order-id", Long.toString(orderId), orderId);
        shardRoutingOperations.execute("processed_message", userId,
                () -> consumerTemplate.consume(event, EventTypes.PAYMENT_SUCCEEDED, this::markOrderPaid));
    }

    private void markOrderPaid(OutboxEvent event) {
        orderService.pay(PaymentEventPayload.from(event).orderId());
    }
}
