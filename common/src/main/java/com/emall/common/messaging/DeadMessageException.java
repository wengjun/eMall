package com.emall.common.messaging;

public class DeadMessageException extends RuntimeException {
    private final String messageId;

    public DeadMessageException(String messageId, Throwable cause) {
        super("message reached its terminal retry limit: " + messageId, cause);
        this.messageId = messageId;
    }

    public String messageId() {
        return messageId;
    }
}
