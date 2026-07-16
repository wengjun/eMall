package com.emall.common.messaging;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.common.event.OutboxEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DuplicateKeyException;

public class MybatisPlusAggregateVersionGuard implements AggregateVersionGuard {
    private static final int MAXIMUM_CAS_ATTEMPTS = 8;
    private final AggregateVersionRecordMapper mapper;
    private final Clock clock;

    public MybatisPlusAggregateVersionGuard(AggregateVersionRecordMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    MybatisPlusAggregateVersionGuard(AggregateVersionRecordMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public AggregateVersionClaim tryAdvance(String consumerName, OutboxEvent event) {
        String claimId = claimId(consumerName, event);
        if (event.aggregateVersion() <= 0) {
            return new AggregateVersionClaim(claimId, event.eventId(), 0L, 0L, false, true);
        }
        for (int attempt = 0; attempt < MAXIMUM_CAS_ATTEMPTS; attempt++) {
            AggregateVersionRecord current = mapper.selectById(claimId);
            if (current == null) {
                try {
                    mapper.insert(record(claimId, consumerName, event));
                    return new AggregateVersionClaim(claimId, event.eventId(), 0L, event.aggregateVersion(), true,
                            true);
                } catch (DuplicateKeyException exception) {
                    continue;
                }
            }
            if (current.getAggregateVersion() >= event.aggregateVersion()) {
                return AggregateVersionClaim.rejected(claimId, event.eventId(), current.getAggregateVersion(),
                        event.aggregateVersion());
            }
            if (event.aggregateVersion() != current.getAggregateVersion() + 1) {
                throw new EventVersionGapException(claimId, current.getAggregateVersion(), event.aggregateVersion());
            }
            int updated = mapper.update(null,
                    new UpdateWrapper<AggregateVersionRecord>().set("aggregate_version", event.aggregateVersion())
                            .set("event_id", event.eventId()).set("updated_at", now())
                            .eq("consumer_aggregate_id", claimId)
                            .eq("aggregate_version", current.getAggregateVersion()));
            if (updated == 1) {
                return new AggregateVersionClaim(claimId, event.eventId(), current.getAggregateVersion(),
                        event.aggregateVersion(), false, true);
            }
        }
        throw new IllegalStateException("consumer aggregate version changed too frequently");
    }

    @Override
    public void rollback(AggregateVersionClaim claim) {
        if (!claim.accepted() || claim.claimedVersion() <= 0) {
            return;
        }
        if (claim.inserted()) {
            mapper.delete(new QueryWrapper<AggregateVersionRecord>().eq("consumer_aggregate_id", claim.claimId())
                    .eq("aggregate_version", claim.claimedVersion()).eq("event_id", claim.eventId()));
            return;
        }
        mapper.update(null,
                new UpdateWrapper<AggregateVersionRecord>().set("aggregate_version", claim.previousVersion())
                        .set("event_id", "rollback").set("updated_at", now())
                        .eq("consumer_aggregate_id", claim.claimId()).eq("aggregate_version", claim.claimedVersion())
                        .eq("event_id", claim.eventId()));
    }

    private AggregateVersionRecord record(String claimId, String consumerName, OutboxEvent event) {
        AggregateVersionRecord record = new AggregateVersionRecord();
        record.setConsumerAggregateId(claimId);
        record.setConsumerName(consumerName);
        record.setAggregateType(event.aggregateType());
        record.setAggregateId(event.aggregateId());
        record.setAggregateVersion(event.aggregateVersion());
        record.setEventId(event.eventId());
        record.setUpdatedAt(now());
        return record;
    }

    private String claimId(String consumerName, OutboxEvent event) {
        return consumerName + ':' + event.aggregateType() + ':' + event.aggregateId();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
