package com.emall.identity;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
class UserProfileEventConsumer {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(EventTypes.USER_PROFILE_READY,
            EventTypes.USER_PROFILE_DELETION_COMPLETED, EventTypes.USER_PROFILE_RECONCILED);

    private final ObjectMapper objectMapper;
    private final AccountLifecycleService lifecycleService;
    private final MessageConsumerTemplate consumerTemplate;

    UserProfileEventConsumer(ObjectMapper objectMapper, AccountLifecycleService lifecycleService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.profile-consumer-max-attempts:8}") int maxAttempts) {
        this(objectMapper, lifecycleService, businessMetrics, processedMessageRepository, maxAttempts, null,
                new InMemoryAggregateVersionGuard());
    }

    @Autowired
    UserProfileEventConsumer(ObjectMapper objectMapper, AccountLifecycleService lifecycleService,
            BusinessMetrics businessMetrics, ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.profile-consumer-max-attempts:8}") int maxAttempts,
            PlatformTransactionManager transactionManager, AggregateVersionGuard aggregateVersionGuard) {
        this.objectMapper = objectMapper;
        this.lifecycleService = lifecycleService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "identity-user-profile-consumer", transactionManager, aggregateVersionGuard);
    }

    @KafkaListener(topics = "${emall.events.user-profile-topic:emall.user-profile.events}",
            groupId = "${spring.kafka.consumer.group-id:identity-lifecycle}")
    void onProfileEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!SUPPORTED_EVENTS.contains(event.eventType())) {
            return;
        }
        consumerTemplate.consume(event, event.eventType(), lifecycleService::handleProfileEvent);
    }
}
