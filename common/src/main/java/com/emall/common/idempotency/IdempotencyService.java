package com.emall.common.idempotency;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

public class IdempotencyService {
    private final IdempotencyRepository repository;
    private final Clock clock;
    private final Duration processingTtl;
    private final Duration recordTtl;

    public IdempotencyService(IdempotencyRepository repository, Clock clock, Duration processingTtl,
            Duration recordTtl) {
        this.repository = repository;
        this.clock = clock;
        this.processingTtl = processingTtl;
        this.recordTtl = recordTtl;
    }

    public IdempotencyStartResult begin(IdempotencyKey key, String businessType, String businessId,
            String requestDigest) {
        Instant now = clock.instant();
        String storageKey = key.storageKey();
        IdempotencyRecord firstRecord = IdempotencyRecord.processing(key, businessType, businessId, requestDigest,
                now.plus(processingTtl), now.plus(recordTtl));
        if (repository.insertProcessing(firstRecord)) {
            return new IdempotencyStartResult(IdempotencyStartStatus.STARTED, firstRecord);
        }
        return existingResult(storageKey, requestDigest, now);
    }

    public IdempotencyRecord markSucceeded(String key, String responseDigest) {
        return transitionFromProcessing(key, responseDigest, IdempotencyStatus.SUCCEEDED);
    }

    public IdempotencyRecord markRetryableFailed(String key, String responseDigest) {
        return transitionFromProcessing(key, responseDigest, IdempotencyStatus.RETRYABLE_FAILED);
    }

    public IdempotencyRecord markTerminalFailed(String key, String responseDigest) {
        return transitionFromProcessing(key, responseDigest, IdempotencyStatus.TERMINAL_FAILED);
    }

    public String digest(String canonicalPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private IdempotencyStartResult existingResult(String key, String requestDigest, Instant now) {
        IdempotencyRecord existing = repository.find(key).orElseThrow(
                () -> new BusinessException(ErrorCode.CONFLICT, "idempotency record was concurrently removed"));
        if (!existing.requestMatches(requestDigest)) {
            throw new BusinessException(ErrorCode.CONFLICT, "idempotency key already used by different request");
        }
        if (existing.status() == IdempotencyStatus.SUCCEEDED) {
            return new IdempotencyStartResult(IdempotencyStartStatus.REPLAY_SUCCEEDED, existing);
        }
        if (existing.status() == IdempotencyStatus.TERMINAL_FAILED) {
            return new IdempotencyStartResult(IdempotencyStartStatus.TERMINAL_FAILED, existing);
        }
        if (existing.processingLockActive(now)) {
            return new IdempotencyStartResult(IdempotencyStartStatus.IN_PROGRESS, existing);
        }
        IdempotencyRecord next = existing.processingAgain(now.plus(processingTtl), now);
        if (repository.replace(key, existing.status(), next)) {
            return new IdempotencyStartResult(IdempotencyStartStatus.STARTED, next);
        }
        return existingResult(key, requestDigest, now);
    }

    private IdempotencyRecord transitionFromProcessing(String key, String responseDigest,
            IdempotencyStatus targetStatus) {
        Instant now = clock.instant();
        IdempotencyRecord existing = repository.find(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "idempotency record not found"));
        IdempotencyRecord next = switch (targetStatus) {
            case SUCCEEDED -> existing.succeeded(responseDigest, now);
            case RETRYABLE_FAILED -> existing.retryableFailed(responseDigest, now);
            case TERMINAL_FAILED -> existing.terminalFailed(responseDigest, now);
            case PROCESSING -> throw new IllegalArgumentException("targetStatus must be terminal or failed");
        };
        if (!repository.replace(key, IdempotencyStatus.PROCESSING, next)) {
            throw new BusinessException(ErrorCode.CONFLICT, "idempotency record is not processing");
        }
        return next;
    }
}
