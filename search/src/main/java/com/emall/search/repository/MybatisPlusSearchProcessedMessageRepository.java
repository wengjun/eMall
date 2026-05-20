package com.emall.search.repository;

import com.emall.common.messaging.MybatisPlusProcessedMessageRepositorySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusSearchProcessedMessageRepository extends MybatisPlusProcessedMessageRepositorySupport
        implements
            ProcessedMessageRepository {
    public MybatisPlusSearchProcessedMessageRepository(SearchProcessedMessageMapper processedMessageMapper) {
        super(processedMessageMapper);
    }
}
