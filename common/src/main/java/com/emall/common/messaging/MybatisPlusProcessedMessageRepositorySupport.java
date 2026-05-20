package com.emall.common.messaging;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DuplicateKeyException;

public abstract class MybatisPlusProcessedMessageRepositorySupport implements ProcessedMessageRepository {
    private final BaseMapper<ProcessedMessageRecord> processedMessageMapper;
    private final Clock clock;

    protected MybatisPlusProcessedMessageRepositorySupport(BaseMapper<ProcessedMessageRecord> processedMessageMapper) {
        this(processedMessageMapper, Clock.systemUTC());
    }

    protected MybatisPlusProcessedMessageRepositorySupport(BaseMapper<ProcessedMessageRecord> processedMessageMapper,
            Clock clock) {
        this.processedMessageMapper = processedMessageMapper;
        this.clock = clock;
    }

    @Override
    public boolean markProcessing(String messageId) {
        ProcessedMessageRecord record = new ProcessedMessageRecord();
        record.setMessageId(messageId);
        LocalDateTime now = now();
        record.setProcessedAt(now);
        record.setStatus(ProcessedMessageStatus.PROCESSING.name());
        record.setRetryCount(0);
        record.setUpdatedAt(now);
        try {
            processedMessageMapper.insert(record);
            return true;
        } catch (DuplicateKeyException ex) {
            return reclaimFailed(messageId, now);
        }
    }

    @Override
    public void markProcessed(String messageId) {
        LocalDateTime now = now();
        processedMessageMapper.update(null,
                new UpdateWrapper<ProcessedMessageRecord>().set("status", ProcessedMessageStatus.PROCESSED.name())
                        .set("processed_at", now).set("last_error_code", null).set("last_error", null)
                        .set("updated_at", now).eq("message_id", messageId));
    }

    @Override
    public int markFailed(String messageId, String errorCode, String lastError) {
        ProcessedMessageRecord current = processedMessageMapper.selectById(messageId);
        int retryCount = current == null || current.getRetryCount() == null ? 1 : current.getRetryCount() + 1;
        LocalDateTime now = now();
        processedMessageMapper.update(null,
                new UpdateWrapper<ProcessedMessageRecord>().set("status", ProcessedMessageStatus.FAILED.name())
                        .set("retry_count", retryCount).set("last_error_code", errorCode).set("last_error", lastError)
                        .set("updated_at", now).eq("message_id", messageId));
        return retryCount;
    }

    @Override
    public void markDead(String messageId, String errorCode, String lastError) {
        LocalDateTime now = now();
        processedMessageMapper.update(null,
                new UpdateWrapper<ProcessedMessageRecord>().set("status", ProcessedMessageStatus.DEAD.name())
                        .set("last_error_code", errorCode).set("last_error", lastError).set("dead_at", now)
                        .set("updated_at", now).eq("message_id", messageId));
    }

    private boolean reclaimFailed(String messageId, LocalDateTime now) {
        LocalDateTime expiredProcessing = now.minusMinutes(5);
        return processedMessageMapper
                .update(null,
                        new UpdateWrapper<ProcessedMessageRecord>()
                                .set("status", ProcessedMessageStatus.PROCESSING.name()).set("updated_at",
                                        now)
                                .eq("message_id",
                                        messageId)
                                .and(wrapper -> wrapper.eq("status", ProcessedMessageStatus.FAILED.name())
                                        .or(nested -> nested.eq("status", ProcessedMessageStatus.PROCESSING.name())
                                                .le("updated_at", expiredProcessing)))) == 1;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
