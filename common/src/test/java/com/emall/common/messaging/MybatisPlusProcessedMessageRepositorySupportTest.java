package com.emall.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

class MybatisPlusProcessedMessageRepositorySupportTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final BaseMapper<ProcessedMessageRecord> mapper = mockMapper();
    private final TestRepository repository = new TestRepository(mapper, FIXED_CLOCK);

    @Test
    void shouldInsertProcessingRecordForFirstDelivery() {
        when(mapper.insert(any(ProcessedMessageRecord.class))).thenReturn(1);

        boolean claimed = repository.markProcessing("message-1");

        ArgumentCaptor<ProcessedMessageRecord> recordCaptor = ArgumentCaptor.forClass(ProcessedMessageRecord.class);
        verify(mapper).insert(recordCaptor.capture());
        ProcessedMessageRecord record = recordCaptor.getValue();
        assertThat(claimed).isTrue();
        assertThat(record.getMessageId()).isEqualTo("message-1");
        assertThat(record.getStatus()).isEqualTo("PROCESSING");
        assertThat(record.getRetryCount()).isZero();
        assertThat(record.getUpdatedAt()).isEqualTo("2026-01-01T00:00");
    }

    @Test
    void shouldReclaimFailedOrExpiredProcessingMessage() {
        when(mapper.insert(any(ProcessedMessageRecord.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(mapper.update(isNull(), anyWrapper())).thenReturn(1);

        boolean claimed = repository.markProcessing("message-1");

        assertThat(claimed).isTrue();
        verify(mapper).update(isNull(), anyWrapper());
    }

    @Test
    void shouldIncrementRetryCountWhenMarkingFailed() {
        ProcessedMessageRecord current = new ProcessedMessageRecord();
        current.setMessageId("message-1");
        current.setRetryCount(2);
        when(mapper.selectById("message-1")).thenReturn(current);

        int retryCount = repository.markFailed("message-1", "IllegalStateException", "failed");

        assertThat(retryCount).isEqualTo(3);
        verify(mapper).update(isNull(), anyWrapper());
    }

    private static final class TestRepository extends MybatisPlusProcessedMessageRepositorySupport {
        private TestRepository(BaseMapper<ProcessedMessageRecord> mapper, Clock clock) {
            super(mapper, clock);
        }
    }

    @SuppressWarnings("unchecked")
    private BaseMapper<ProcessedMessageRecord> mockMapper() {
        return mock(BaseMapper.class);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<ProcessedMessageRecord> anyWrapper() {
        return any(Wrapper.class);
    }
}
