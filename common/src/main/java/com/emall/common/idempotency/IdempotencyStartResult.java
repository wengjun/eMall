package com.emall.common.idempotency;

public record IdempotencyStartResult(IdempotencyStartStatus status, IdempotencyRecord record) {
    public boolean shouldExecute() {
        return status == IdempotencyStartStatus.STARTED;
    }
}
