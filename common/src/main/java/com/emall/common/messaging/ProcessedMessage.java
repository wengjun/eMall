package com.emall.common.messaging;

import java.time.Instant;

public record ProcessedMessage(String messageId, ProcessedMessageStatus status, int retryCount, String lastErrorCode,
        String lastError, Instant updatedAt, Instant deadAt) {
}
