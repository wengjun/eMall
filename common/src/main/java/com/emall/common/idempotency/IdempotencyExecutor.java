package com.emall.common.idempotency;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class IdempotencyExecutor {
    private IdempotencyExecutor() {
    }

    public static <T> T execute(IdempotencyService service, IdempotencyKey key, String businessType, String businessId,
            String requestDigest, Supplier<T> action, Function<IdempotencyRecord, T> replay,
            Function<T, String> responseDigest) {
        Objects.requireNonNull(service, "service must not be null");
        IdempotencyStartResult result = service.begin(key, businessType, businessId, requestDigest);
        return switch (result.status()) {
            case REPLAY_SUCCEEDED -> replay.apply(result.record());
            case IN_PROGRESS ->
                throw new BusinessException(ErrorCode.CONFLICT, "idempotent request is still being processed");
            case TERMINAL_FAILED ->
                throw new BusinessException(ErrorCode.CONFLICT, "idempotent request has already failed terminally");
            case STARTED -> executeStarted(service, result.record().key(), action, responseDigest);
        };
    }

    private static <T> T executeStarted(IdempotencyService service, String storageKey, Supplier<T> action,
            Function<T, String> responseDigest) {
        try {
            T value = action.get();
            service.markSucceeded(storageKey, responseDigest.apply(value));
            return value;
        } catch (BusinessException ex) {
            service.markTerminalFailed(storageKey, service.digest(ex.errorCode() + ":" + ex.getMessage()));
            throw ex;
        } catch (RuntimeException ex) {
            service.markRetryableFailed(storageKey, service.digest(ex.getClass().getName() + ":" + ex.getMessage()));
            throw ex;
        }
    }
}
