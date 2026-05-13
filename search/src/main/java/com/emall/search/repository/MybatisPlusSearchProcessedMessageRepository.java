package com.emall.search.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusSearchProcessedMessageRepository implements ProcessedMessageRepository {
    private final SearchProcessedMessageMapper processedMessageMapper;

    public MybatisPlusSearchProcessedMessageRepository(SearchProcessedMessageMapper processedMessageMapper) {
        this.processedMessageMapper = processedMessageMapper;
    }

    @Override
    public boolean markProcessing(String messageId) {
        SearchProcessedMessageEntity entity = new SearchProcessedMessageEntity();
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
