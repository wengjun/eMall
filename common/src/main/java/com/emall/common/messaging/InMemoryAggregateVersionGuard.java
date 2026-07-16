package com.emall.common.messaging;

import com.emall.common.event.OutboxEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAggregateVersionGuard implements AggregateVersionGuard {
    private final ConcurrentMap<String, VersionState> versions = new ConcurrentHashMap<>();

    @Override
    public AggregateVersionClaim tryAdvance(String consumerName, OutboxEvent event) {
        String claimId = claimId(consumerName, event);
        if (event.aggregateVersion() <= 0) {
            return new AggregateVersionClaim(claimId, event.eventId(), 0L, 0L, false, true);
        }
        ClaimBox box = new ClaimBox();
        versions.compute(claimId, (ignored, current) -> {
            long previous = current == null ? 0L : current.version();
            if (previous >= event.aggregateVersion()) {
                box.claim =
                        AggregateVersionClaim.rejected(claimId, event.eventId(), previous, event.aggregateVersion());
                return current;
            }
            if (current != null && event.aggregateVersion() != previous + 1) {
                throw new EventVersionGapException(claimId, previous, event.aggregateVersion());
            }
            box.claim = new AggregateVersionClaim(claimId, event.eventId(), previous, event.aggregateVersion(),
                    current == null, true);
            return new VersionState(event.aggregateVersion(), event.eventId());
        });
        return box.claim;
    }

    @Override
    public void rollback(AggregateVersionClaim claim) {
        if (!claim.accepted() || claim.claimedVersion() <= 0) {
            return;
        }
        versions.computeIfPresent(claim.claimId(), (ignored, current) -> {
            if (current.version() != claim.claimedVersion() || !current.eventId().equals(claim.eventId())) {
                return current;
            }
            return claim.previousVersion() == 0 ? null : new VersionState(claim.previousVersion(), "rollback");
        });
    }

    private String claimId(String consumerName, OutboxEvent event) {
        return consumerName + ':' + event.aggregateType() + ':' + event.aggregateId();
    }

    private record VersionState(long version, String eventId) {
    }

    private static final class ClaimBox {
        private AggregateVersionClaim claim;
    }
}
