package com.emall.common.outbox;

import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import java.time.Duration;
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
    public List<OutboxEvent> claimPublishable(String ownerId, Instant now, Duration leaseTtl, int limit) {
        return events.values().stream().filter(event -> publishable(event, now))
                .sorted(Comparator.comparing(OutboxEvent::createdAt)).limit(limit)
                .map(event -> claimOne(event.eventId(), ownerId, now.plus(leaseTtl), now)).flatMap(List::stream)
                .toList();
    }

    @Override
    public List<OutboxEvent> findPublishable(Instant now, int limit) {
        return events.values().stream().filter(event -> publishable(event, now))
                .sorted(Comparator.comparing(OutboxEvent::createdAt)).limit(limit).toList();
    }

    @Override
    public int rescheduleFailed(Instant now, int limit) {
        List<OutboxEvent> failedEvents = events.values().stream().filter(event -> event.status() == OutboxStatus.FAILED)
                .sorted(Comparator.comparing(OutboxEvent::createdAt)).limit(limit).toList();
        failedEvents.forEach(event -> events.put(event.eventId(), event.readyForRetry(now)));
        return failedEvents.size();
    }

    private List<OutboxEvent> claimOne(String eventId, String ownerId, Instant claimedUntil, Instant now) {
        AtomicBox<OutboxEvent> claimed = new AtomicBox<>();
        events.computeIfPresent(eventId, (key, existing) -> {
            if (!publishable(existing, now)) {
                return existing;
            }
            OutboxEvent next = existing.claimed(ownerId, claimedUntil);
            claimed.set(next);
            return next;
        });
        return claimed.value() == null ? List.of() : List.of(claimed.value());
    }

    private boolean publishable(OutboxEvent event, Instant now) {
        if ((event.status() == OutboxStatus.NEW || event.status() == OutboxStatus.FAILED)
                && !event.nextRetryAt().isAfter(now)) {
            return true;
        }
        return event.status() == OutboxStatus.PROCESSING && event.claimedUntil() != null
                && !event.claimedUntil().isAfter(now);
    }

    private static final class AtomicBox<T> {
        private T value;

        void set(T value) {
            this.value = value;
        }

        T value() {
            return value;
        }
    }
}
