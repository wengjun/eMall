package com.emall.search.repository;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryProcessedMessageRepository extends com.emall.common.messaging.InMemoryProcessedMessageRepository
        implements
            ProcessedMessageRepository {
    public InMemoryProcessedMessageRepository() {
        super();
    }

    public InMemoryProcessedMessageRepository(Clock clock) {
        super(clock);
    }
}
