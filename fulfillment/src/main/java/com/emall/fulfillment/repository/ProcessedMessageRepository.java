package com.emall.fulfillment.repository;

public interface ProcessedMessageRepository {
    boolean markProcessing(String messageId);
}
