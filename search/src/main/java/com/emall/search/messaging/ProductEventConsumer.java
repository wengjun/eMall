package com.emall.search.messaging;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.search.repository.ProcessedMessageRepository;
import com.emall.search.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {
    private final SearchService searchService;
    private final BusinessMetrics businessMetrics;
    private final MessageConsumerTemplate consumerTemplate;

    public ProductEventConsumer(ObjectMapper objectMapper, SearchService searchService,
            ProcessedMessageRepository processedMessageRepository, BusinessMetrics businessMetrics,
            @Value("${emall.events.product-consumer-max-attempts:4}") int maxAttempts) {
        this.searchService = searchService;
        this.businessMetrics = businessMetrics;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "search-product-indexer");
    }

    @KafkaListener(topics = "${emall.events.product-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onProductEvent(String message) throws JsonProcessingException {
        consumerTemplate.consume(message, EventTypes.PRODUCT_CHANGED, this::indexProduct);
    }

    private void indexProduct(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        String category = stringValue(payload.get("category"));
        searchService.index(longValue(payload.get("skuId")), stringValue(payload.get("title")), category,
                decimalValue(payload.get("price")), Set.of(category), booleanValue(payload.get("saleable")),
                longValue(payload.getOrDefault("version", event.createdAt().toEpochMilli())));
        businessMetrics.increment(BusinessMetricNames.SEARCH_PRODUCT_EVENT_INDEXED, "event_type", event.eventType());
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return String.valueOf(value);
    }

}
