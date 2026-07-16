package com.emall.user.repository;

import com.emall.common.outbox.MybatisPlusOutboxRepositorySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusUserOutboxRepository extends MybatisPlusOutboxRepositorySupport {
    public MybatisPlusUserOutboxRepository(UserOutboxEventMapper mapper, ObjectMapper objectMapper) {
        super(mapper, objectMapper);
    }
}
