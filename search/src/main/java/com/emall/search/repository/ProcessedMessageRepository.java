package com.emall.search.repository;

public interface ProcessedMessageRepository {
    boolean markProcessing(String messageId);
}
