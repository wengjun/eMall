package com.emall.search.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusSearchProcessedMessageRepository implements ProcessedMessageRepository {
    private final SearchProcessedMessageMapper processedMessageMapper;

    public MybatisPlusSearchProcessedMessageRepository(SearchProcessedMessageMapper processedMessageMapper) {
        this.processedMessageMapper = processedMessageMapper;
    }

    @Override
    public boolean markProcessing(String messageId) {
        SearchProcessedMessageEntity entity = new SearchProcessedMessageEntity();
        entity.setMessageId(messageId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        entity.setProcessedAt(now);
        entity.setStatus(ProcessedMessageStatus.PROCESSING.name());
        entity.setRetryCount(0);
        entity.setUpdatedAt(now);
        try {
            processedMessageMapper.insert(entity);
            return true;
        } catch (DuplicateKeyException ex) {
            return reclaimFailed(messageId, now);
        }
    }

    @Override
    public void markProcessed(String messageId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        processedMessageMapper.update(null,
                new UpdateWrapper<SearchProcessedMessageEntity>().set("status", ProcessedMessageStatus.PROCESSED.name())
                        .set("processed_at", now).set("last_error_code", null).set("last_error", null)
                        .set("updated_at", now).eq("message_id", messageId));
    }

    @Override
    public int markFailed(String messageId, String errorCode, String lastError) {
        SearchProcessedMessageEntity current = processedMessageMapper.selectById(messageId);
        int retryCount = current == null || current.getRetryCount() == null ? 1 : current.getRetryCount() + 1;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        processedMessageMapper.update(null,
                new UpdateWrapper<SearchProcessedMessageEntity>().set("status", ProcessedMessageStatus.FAILED.name())
                        .set("retry_count", retryCount).set("last_error_code", errorCode).set("last_error", lastError)
                        .set("updated_at", now).eq("message_id", messageId));
        return retryCount;
    }

    @Override
    public void markDead(String messageId, String errorCode, String lastError) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        processedMessageMapper.update(null,
                new UpdateWrapper<SearchProcessedMessageEntity>().set("status", ProcessedMessageStatus.DEAD.name())
                        .set("last_error_code", errorCode).set("last_error", lastError).set("dead_at", now)
                        .set("updated_at", now).eq("message_id", messageId));
    }

    private boolean reclaimFailed(String messageId, LocalDateTime now) {
        LocalDateTime expiredProcessing = now.minusMinutes(5);
        return processedMessageMapper
                .update(null,
                        new UpdateWrapper<SearchProcessedMessageEntity>()
                                .set("status", ProcessedMessageStatus.PROCESSING.name()).set("updated_at",
                                        now)
                                .eq("message_id",
                                        messageId)
                                .and(wrapper -> wrapper.eq("status", ProcessedMessageStatus.FAILED.name())
                                        .or(nested -> nested.eq("status", ProcessedMessageStatus.PROCESSING.name())
                                                .le("updated_at", expiredProcessing)))) == 1;
    }
}
