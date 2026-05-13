package com.emall.fulfillment.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusFulfillmentProcessedMessageRepository implements ProcessedMessageRepository {
    private final FulfillmentProcessedMessageMapper processedMessageMapper;

    public MybatisPlusFulfillmentProcessedMessageRepository(
            FulfillmentProcessedMessageMapper processedMessageMapper) {
        this.processedMessageMapper = processedMessageMapper;
    }

    @Override
    public boolean markProcessing(String messageId) {
        FulfillmentProcessedMessageEntity entity = new FulfillmentProcessedMessageEntity();
        entity.setMessageId(messageId);
        entity.setProcessedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            processedMessageMapper.insert(entity);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}
