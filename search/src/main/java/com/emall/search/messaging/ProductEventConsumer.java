package com.emall.search.messaging;

import com.emall.common.event.EventContractRegistry;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.ProductChangedEventPayload;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.AggregateVersionGuard;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.search.repository.ProcessedMessageRepository;
import com.emall.search.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class ProductEventConsumer {
    private final SearchService searchService;
    private final BusinessMetrics businessMetrics;
    private final MessageConsumerTemplate consumerTemplate;
    private final ObjectMapper objectMapper;
    private final ShardRoutingOperations shardRoutingOperations;

    public ProductEventConsumer(ObjectMapper objectMapper, SearchService searchService,
            ProcessedMessageRepository processedMessageRepository, BusinessMetrics businessMetrics,
            @Value("${emall.events.product-consumer-max-attempts:4}") int maxAttempts) {
        this(objectMapper, searchService, processedMessageRepository, businessMetrics, maxAttempts, null,
                ShardRoutingOperations.noop(), new InMemoryAggregateVersionGuard());
    }

    @Autowired
    public ProductEventConsumer(ObjectMapper objectMapper, SearchService searchService,
            ProcessedMessageRepository processedMessageRepository, BusinessMetrics businessMetrics,
            @Value("${emall.events.product-consumer-max-attempts:4}") int maxAttempts,
            PlatformTransactionManager transactionManager, ShardRoutingOperations shardRoutingOperations,
            AggregateVersionGuard aggregateVersionGuard) {
        this.searchService = searchService;
        this.businessMetrics = businessMetrics;
        this.objectMapper = objectMapper;
        this.shardRoutingOperations = shardRoutingOperations;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "search-product-indexer", transactionManager, aggregateVersionGuard);
    }

    @KafkaListener(topics = "${emall.events.product-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onProductEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!EventTypes.PRODUCT_CHANGED.equals(event.eventType())) {
            consumerTemplate.consume(event, EventTypes.PRODUCT_CHANGED, this::indexProduct);
            return;
        }
        EventContractRegistry.validate(event);
        long skuId = ProductChangedEventPayload.from(event).skuId();
        shardRoutingOperations.execute("processed_message", skuId,
                () -> consumerTemplate.consume(event, EventTypes.PRODUCT_CHANGED, this::indexProduct));
    }

    private void indexProduct(OutboxEvent event) {
        ProductChangedEventPayload payload = ProductChangedEventPayload.from(event);
        long version = event.aggregateVersion() > 0 ? event.aggregateVersion() : payload.updatedAt().toEpochMilli();
        searchService.index(payload.skuId(), payload.title(), payload.category(), payload.price(),
                Set.of(payload.category()), payload.saleable(), version);
        businessMetrics.increment(BusinessMetricNames.SEARCH_PRODUCT_EVENT_INDEXED, "event_type", event.eventType());
    }
}
