package com.emall.common.outbox;

import com.emall.common.event.OutboxEvent;
import java.time.Instant;
import java.util.List;

public interface OutboxRepository {
    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPublishable(Instant now, int limit);

    int rescheduleFailed(Instant now, int limit);
}
