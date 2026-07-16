package com.emall.common.messaging;

import com.emall.common.event.OutboxEvent;

public interface AggregateVersionGuard {
    AggregateVersionClaim tryAdvance(String consumerName, OutboxEvent event);

    void rollback(AggregateVersionClaim claim);
}
