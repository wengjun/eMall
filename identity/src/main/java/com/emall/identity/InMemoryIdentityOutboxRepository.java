package com.emall.identity;

import com.emall.common.outbox.InMemoryOutboxRepositorySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryIdentityOutboxRepository extends InMemoryOutboxRepositorySupport {
}
