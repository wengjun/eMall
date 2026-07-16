package com.emall.common.messaging;

import com.emall.common.event.EventContractRegistry;
import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class MessageConsumerTemplate {
    private final ObjectMapper objectMapper;
    private final ProcessedMessageRepository repository;
    private final BusinessMetrics businessMetrics;
    private final int maxAttempts;
    private final String consumerName;
    private final AggregateVersionGuard aggregateVersionGuard;
    private final TransactionTemplate businessTransaction;
    private final TransactionTemplate failureTransaction;

    public MessageConsumerTemplate(ObjectMapper objectMapper, ProcessedMessageRepository repository,
            BusinessMetrics businessMetrics, int maxAttempts, String consumerName) {
        this(objectMapper, repository, businessMetrics, maxAttempts, consumerName, null,
                new InMemoryAggregateVersionGuard());
    }

    public MessageConsumerTemplate(ObjectMapper objectMapper, ProcessedMessageRepository repository,
            BusinessMetrics businessMetrics, int maxAttempts, String consumerName,
            PlatformTransactionManager transactionManager) {
        this(objectMapper, repository, businessMetrics, maxAttempts, consumerName, transactionManager,
                new InMemoryAggregateVersionGuard());
    }

    public MessageConsumerTemplate(ObjectMapper objectMapper, ProcessedMessageRepository repository,
            BusinessMetrics businessMetrics, int maxAttempts, String consumerName,
            PlatformTransactionManager transactionManager, AggregateVersionGuard aggregateVersionGuard) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.businessMetrics = businessMetrics;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.consumerName = consumerName;
        this.aggregateVersionGuard = aggregateVersionGuard;
        if (transactionManager == null) {
            this.businessTransaction = null;
            this.failureTransaction = null;
        } else {
            this.businessTransaction = new TransactionTemplate(transactionManager);
            this.businessTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.failureTransaction = new TransactionTemplate(transactionManager);
            this.failureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    public ConsumerExecutionResult consume(String message, String expectedEventType, Consumer<OutboxEvent> handler)
            throws JsonProcessingException {
        OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
        return consume(event, expectedEventType, handler);
    }

    public ConsumerExecutionResult consume(OutboxEvent event, String expectedEventType, Consumer<OutboxEvent> handler) {
        if (!expectedEventType.equals(event.eventType())) {
            return ConsumerExecutionResult.IGNORED;
        }
        try {
            return executeBusiness(() -> {
                EventContractRegistry.validate(event);
                return consumeInTransaction(event, handler);
            });
        } catch (RuntimeException ex) {
            int retryCount = executeFailure(() -> recordFailure(event, ex));
            if (retryCount >= maxAttempts) {
                throw new DeadMessageException(event.eventId(), ex);
            }
            throw ex;
        }
    }

    private ConsumerExecutionResult consumeInTransaction(OutboxEvent event, Consumer<OutboxEvent> handler) {
        if (!repository.markProcessing(event.eventId())) {
            businessMetrics.increment(BusinessMetricNames.MESSAGE_DUPLICATED, "consumer", consumerName, "event_type",
                    event.eventType());
            return ConsumerExecutionResult.DUPLICATED;
        }
        AggregateVersionClaim versionClaim = aggregateVersionGuard.tryAdvance(consumerName, event);
        if (!versionClaim.accepted()) {
            repository.markProcessed(event.eventId());
            businessMetrics.increment(BusinessMetricNames.MESSAGE_STALE, "consumer", consumerName, "event_type",
                    event.eventType());
            return ConsumerExecutionResult.STALE;
        }
        try {
            handler.accept(event);
            repository.markProcessed(event.eventId());
            businessMetrics.increment(BusinessMetricNames.MESSAGE_CONSUMED, "consumer", consumerName, "event_type",
                    event.eventType());
            return ConsumerExecutionResult.PROCESSED;
        } catch (RuntimeException exception) {
            aggregateVersionGuard.rollback(versionClaim);
            throw exception;
        }
    }

    private int recordFailure(OutboxEvent event, RuntimeException ex) {
        int retryCount = repository.markFailed(event.eventId(), errorCode(ex), safeMessage(ex));
        businessMetrics.increment(BusinessMetricNames.MESSAGE_FAILED, "consumer", consumerName, "event_type",
                event.eventType());
        if (retryCount >= maxAttempts) {
            repository.markDead(event.eventId(), errorCode(ex), safeMessage(ex));
            businessMetrics.increment(BusinessMetricNames.MESSAGE_DEAD, "consumer", consumerName, "event_type",
                    event.eventType());
        }
        return retryCount;
    }

    private <T> T executeBusiness(java.util.function.Supplier<T> action) {
        if (businessTransaction == null) {
            return action.get();
        }
        return businessTransaction.execute(status -> action.get());
    }

    private int executeFailure(java.util.function.IntSupplier action) {
        if (failureTransaction == null) {
            return action.getAsInt();
        }
        Integer result = failureTransaction.execute(status -> action.getAsInt());
        return result == null ? 0 : result;
    }

    private String errorCode(RuntimeException ex) {
        return ex.getClass().getSimpleName();
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
