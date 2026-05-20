package com.emall.common.idempotency;

public enum IdempotencyStartStatus {
    STARTED,
    IN_PROGRESS,
    REPLAY_SUCCEEDED,
    TERMINAL_FAILED
}
