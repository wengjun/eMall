package com.emall.search.messaging;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.search.repository.ProcessedMessageRepository;
import com.emall.search.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductEventConsumer {
    private final ObjectMapper objectMapper;
    private final SearchService searchService;
    private final ProcessedMessageRepository processedMessageRepository;

    public ProductEventConsumer(ObjectMapper objectMapper,
                                SearchService searchService,
                                ProcessedMessageRepository processedMessageRepository) {
        this.objectMapper = objectMapper;
        this.searchService = searchService;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Transactional
    @KafkaListener(topics = "${emall.events.product-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onProductEvent(String message) throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!EventTypes.PRODUCT_CHANGED.equals(event.eventType())) {
            return;
        }
        if (!processedMessageRepository.markProcessing(event.eventId())) {
            return;
        }
        Map<String, Object> payload = event.payload();
        String category = stringValue(payload.get("category"));
        searchService.index(
                longValue(payload.get("skuId")),
                stringValue(payload.get("title")),
                category,
                decimalValue(payload.get("price")),
                Set.of(category),
                booleanValue(payload.get("saleable")));
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
