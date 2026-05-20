package com.emall.common.event;

public enum OutboxStatus {
    NEW,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DEAD
}
