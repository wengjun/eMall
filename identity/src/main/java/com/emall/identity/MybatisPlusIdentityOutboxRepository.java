package com.emall.identity;

import com.emall.common.outbox.MybatisPlusOutboxRepositorySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusIdentityOutboxRepository extends MybatisPlusOutboxRepositorySupport {
    MybatisPlusIdentityOutboxRepository(IdentityOutboxEventMapper mapper, ObjectMapper objectMapper) {
        super(mapper, objectMapper);
    }
}
