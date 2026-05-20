package com.emall.fulfillment.repository;

import com.emall.common.messaging.MybatisPlusProcessedMessageRepositorySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusFulfillmentProcessedMessageRepository extends MybatisPlusProcessedMessageRepositorySupport
        implements
            ProcessedMessageRepository {
    public MybatisPlusFulfillmentProcessedMessageRepository(FulfillmentProcessedMessageMapper processedMessageMapper) {
        super(processedMessageMapper);
    }
}
