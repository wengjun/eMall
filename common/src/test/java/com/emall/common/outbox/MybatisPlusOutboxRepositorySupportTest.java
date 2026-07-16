package com.emall.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

class MybatisPlusOutboxRepositorySupportTest {
    private final OutboxEventMapper mapper = mock(OutboxEventMapper.class);
    private final MybatisPlusOutboxRepositorySupport repository =
            new MybatisPlusOutboxRepositorySupport(mapper, new ObjectMapper().findAndRegisterModules()) {
            };

    @Test
    void shouldClaimEventIdBeforeAllocatingAggregateVersion() {
        when(mapper.selectById("event-1")).thenReturn(null);
        when(mapper.insert(any(OutboxEventRecord.class))).thenReturn(1);
        when(mapper.advanceAggregateVersion(any(), any())).thenReturn(1);
        when(mapper.currentAggregateVersion("Order:1001")).thenReturn(1L);
        when(mapper.update(nullable(OutboxEventRecord.class), any())).thenReturn(1);

        OutboxEvent saved = repository.save(event("event-1"));

        ArgumentCaptor<OutboxEventRecord> inserted = ArgumentCaptor.forClass(OutboxEventRecord.class);
        verify(mapper).insert(inserted.capture());
        assertThat(inserted.getValue().getAggregateVersion()).isZero();
        assertThat(saved.aggregateVersion()).isOne();
        verify(mapper).advanceAggregateVersion(any(), any());
        verify(mapper).update(nullable(OutboxEventRecord.class), any());
    }

    @Test
    void duplicateEventIdMustReturnExistingVersionWithoutConsumingAnotherVersion() {
        OutboxEvent existing = event("event-1").withAggregateVersion(7);
        when(mapper.selectById("event-1")).thenReturn(null, record(existing));
        when(mapper.insert(any(OutboxEventRecord.class))).thenThrow(new DuplicateKeyException("duplicate"));

        OutboxEvent saved = repository.save(event("event-1"));

        assertThat(saved.aggregateVersion()).isEqualTo(7);
        verify(mapper, never()).advanceAggregateVersion(any(), any());
    }

    private OutboxEvent event(String eventId) {
        OrderEventPayload payload =
                new OrderEventPayload(1001L, 2001L, 3001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-1", "CREATED");
        return OutboxEvent.create(eventId, "Order", "1001", EventTypes.ORDER_CREATED, "order", "1.0.0", payload);
    }

    private OutboxEventRecord record(OutboxEvent event) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 0, 0);
        OutboxEventRecord record = new OutboxEventRecord();
        record.setEventId(event.eventId());
        record.setAggregateType(event.aggregateType());
        record.setAggregateId(event.aggregateId());
        record.setEventType(event.eventType());
        record.setSchemaVersion(event.schemaVersion());
        record.setAggregateVersion(event.aggregateVersion());
        record.setProducer(event.producer());
        record.setProducerVersion(event.producerVersion());
        record.setOccurredAt(now);
        record.setPayload("{\"orderId\":1001}");
        record.setStatus(OutboxStatus.NEW.name());
        record.setRetryCount(0);
        record.setNextRetryAt(now);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }
}
