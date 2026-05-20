package com.emall.flashsale.repository;

import com.emall.common.outbox.MybatisPlusOutboxRepositorySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusOutboxRepository extends MybatisPlusOutboxRepositorySupport {
    public MybatisPlusOutboxRepository(FlashSaleOutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        super(outboxEventMapper, objectMapper);
    }
}
