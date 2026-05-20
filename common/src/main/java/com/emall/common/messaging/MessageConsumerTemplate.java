package com.emall.common.messaging;

import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;

public class MessageConsumerTemplate {
    private final ObjectMapper objectMapper;
    private final ProcessedMessageRepository repository;
    private final BusinessMetrics businessMetrics;
    private final int maxAttempts;
    private final String consumerName;

    public MessageConsumerTemplate(ObjectMapper objectMapper, ProcessedMessageRepository repository,
            BusinessMetrics businessMetrics, int maxAttempts, String consumerName) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.businessMetrics = businessMetrics;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.consumerName = consumerName;
    }

    public ConsumerExecutionResult consume(String message, String expectedEventType, Consumer<OutboxEvent> handler)
            throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        if (!expectedEventType.equals(event.eventType())) {
            return ConsumerExecutionResult.IGNORED;
        }
        if (!repository.markProcessing(event.eventId())) {
            businessMetrics.increment(BusinessMetricNames.MESSAGE_DUPLICATED, "consumer", consumerName, "event_type",
                    event.eventType());
            return ConsumerExecutionResult.DUPLICATED;
        }
        try {
            handler.accept(event);
            repository.markProcessed(event.eventId());
            businessMetrics.increment(BusinessMetricNames.MESSAGE_CONSUMED, "consumer", consumerName, "event_type",
                    event.eventType());
            return ConsumerExecutionResult.PROCESSED;
        } catch (RuntimeException ex) {
            int retryCount = repository.markFailed(event.eventId(), errorCode(ex), safeMessage(ex));
            businessMetrics.increment(BusinessMetricNames.MESSAGE_FAILED, "consumer", consumerName, "event_type",
                    event.eventType());
            if (retryCount >= maxAttempts) {
                repository.markDead(event.eventId(), errorCode(ex), safeMessage(ex));
                businessMetrics.increment(BusinessMetricNames.MESSAGE_DEAD, "consumer", consumerName, "event_type",
                        event.eventType());
            }
            throw ex;
        }
    }

    private String errorCode(RuntimeException ex) {
        return ex.getClass().getSimpleName();
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
