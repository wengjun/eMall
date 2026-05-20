package com.emall.search.repository;

public interface ProcessedMessageRepository {
    boolean markProcessing(String messageId);

    void markProcessed(String messageId);

    int markFailed(String messageId, String errorCode, String lastError);

    void markDead(String messageId, String errorCode, String lastError);
}
