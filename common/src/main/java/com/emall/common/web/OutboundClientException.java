package com.emall.common.web;

public class OutboundClientException extends RuntimeException {
    private final OutboundClientErrorCategory category;
    private final String clientName;

    public OutboundClientException(String clientName, OutboundClientErrorCategory category, String message,
            Throwable cause) {
        super(message, cause);
        this.clientName = clientName;
        this.category = category;
    }

    public OutboundClientErrorCategory category() {
        return category;
    }

    public String clientName() {
        return clientName;
    }
}
