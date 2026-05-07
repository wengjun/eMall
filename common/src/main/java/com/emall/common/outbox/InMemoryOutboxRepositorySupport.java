package com.emall.common.outbox;

import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class InMemoryOutboxRepositorySupport implements OutboxRepository {
    private final ConcurrentMap<String, OutboxEvent> events = new ConcurrentHashMap<>();

    @Override
    public OutboxEvent save(OutboxEvent event) {
        events.put(event.eventId(), event);
        return event;
    }

    @Override
    public List<OutboxEvent> findPublishable(Instant now, int limit) {
        return events.values().stream()
                .filter(event -> event.status() == OutboxStatus.NEW || event.status() == OutboxStatus.FAILED)
                .filter(event -> !event.nextRetryAt().isAfter(now)).sorted(Comparator.comparing(OutboxEvent::createdAt))
                .limit(limit).toList();
    }

    @Override
    public int rescheduleFailed(Instant now, int limit) {
        List<OutboxEvent> failedEvents = events.values().stream().filter(event -> event.status() == OutboxStatus.FAILED)
                .sorted(Comparator.comparing(OutboxEvent::createdAt)).limit(limit).toList();
        failedEvents.forEach(event -> events.put(event.eventId(), event.readyForRetry(now)));
        return failedEvents.size();
    }
}
