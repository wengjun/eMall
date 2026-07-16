package com.emall.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MybatisPlusAggregateVersionGuardTest {
    private final AggregateVersionRecordMapper mapper = mock(AggregateVersionRecordMapper.class);
    private final MybatisPlusAggregateVersionGuard guard = new MybatisPlusAggregateVersionGuard(mapper);

    @Test
    void shouldInsertFirstObservedVersionAsConsumerBaseline() {
        when(mapper.selectById("consumer-a:Order:1001")).thenReturn(null);
        when(mapper.insert(any(AggregateVersionRecord.class))).thenReturn(1);

        AggregateVersionClaim claim = guard.tryAdvance("consumer-a", event("event-5", 5));

        assertThat(claim.accepted()).isTrue();
        assertThat(claim.inserted()).isTrue();
        assertThat(claim.previousVersion()).isZero();
        assertThat(claim.claimedVersion()).isEqualTo(5);
    }

    @Test
    void shouldAdvanceOnlyToTheNextVersion() {
        when(mapper.selectById("consumer-a:Order:1001")).thenReturn(record(5));
        when(mapper.update(nullable(AggregateVersionRecord.class), any())).thenReturn(1);

        AggregateVersionClaim claim = guard.tryAdvance("consumer-a", event("event-6", 6));

        assertThat(claim.accepted()).isTrue();
        assertThat(claim.previousVersion()).isEqualTo(5);
        assertThat(claim.claimedVersion()).isEqualTo(6);
    }

    @Test
    void shouldRejectStaleVersionAndFailOnGap() {
        when(mapper.selectById("consumer-a:Order:1001")).thenReturn(record(5));

        assertThat(guard.tryAdvance("consumer-a", event("event-4", 4)).accepted()).isFalse();
        assertThatThrownBy(() -> guard.tryAdvance("consumer-a", event("event-7", 7)))
                .isInstanceOf(EventVersionGapException.class).hasMessageContaining("current=5, requested=7");
    }

    private AggregateVersionRecord record(long version) {
        AggregateVersionRecord record = new AggregateVersionRecord();
        record.setConsumerAggregateId("consumer-a:Order:1001");
        record.setAggregateVersion(version);
        record.setEventId("event-" + version);
        return record;
    }

    private OutboxEvent event(String eventId, long version) {
        OrderEventPayload payload =
                new OrderEventPayload(1001L, 2001L, 3001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-1", "PAID");
        return OutboxEvent.create(eventId, "Order", "1001", EventTypes.ORDER_PAID, "order", "1.0.0", payload)
                .withAggregateVersion(version);
    }
}
