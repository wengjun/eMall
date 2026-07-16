package com.emall.user.messaging;

import com.emall.common.event.OutboxEvent;
import com.emall.common.event.AccountLifecycleEventPayload;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.user.service.UserLifecycleProjectionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class IdentityAccountEventConsumer {
    private final ObjectMapper objectMapper;
    private final UserLifecycleProjectionService projectionService;
    private final MessageConsumerTemplate consumerTemplate;
    private final ShardRoutingOperations shardRoutingOperations;

    public IdentityAccountEventConsumer(ObjectMapper objectMapper, UserLifecycleProjectionService projectionService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.identity-consumer-max-attempts:8}") int maxAttempts) {
        this(objectMapper, projectionService, businessMetrics, processedMessageRepository, maxAttempts, null,
                new InMemoryAggregateVersionGuard(), ShardRoutingOperations.noop());
    }

    @Autowired
    public IdentityAccountEventConsumer(ObjectMapper objectMapper, UserLifecycleProjectionService projectionService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.identity-consumer-max-attempts:8}") int maxAttempts,
            PlatformTransactionManager transactionManager, AggregateVersionGuard aggregateVersionGuard,
            ShardRoutingOperations shardRoutingOperations) {
        this.objectMapper = objectMapper;
        this.projectionService = projectionService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "user-identity-account-consumer", transactionManager, aggregateVersionGuard);
        this.shardRoutingOperations = shardRoutingOperations;
    }

    @KafkaListener(topics = "${emall.events.identity-topic:emall.identity.events}",
            groupId = "${spring.kafka.consumer.group-id:user-lifecycle}")
    public void onIdentityEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!UserLifecycleProjectionService.SUPPORTED_EVENTS.contains(event.eventType())) {
            return;
        }
        long accountId = AccountLifecycleEventPayload.from(event).accountId();
        shardRoutingOperations.execute("processed_message", accountId, () -> {
            consumerTemplate.consume(event, event.eventType(), projectionService::apply);
            return null;
        });
    }
}
