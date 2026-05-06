package com.emall.fulfillment.repository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryProcessedMessageRepository implements ProcessedMessageRepository {
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markProcessing(String messageId) {
        return processed.add(messageId);
    }
}
