package com.emall.common.controlplane;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class ControlPlaneReconciler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlPlaneReconciler.class);
    private static final int MAX_ERROR_LENGTH = 2000;

    private final ControlPlaneOperationStore store;
    private final Map<ControlPlaneTarget, ControlPlaneAdapter> adapters;
    private final ControlPlaneProperties properties;
    private final Clock clock;
    private final String owner;

    public ControlPlaneReconciler(ControlPlaneOperationStore store, List<ControlPlaneAdapter> adapters,
            ControlPlaneProperties properties, Clock clock) {
        this.store = store;
        this.adapters = new EnumMap<>(ControlPlaneTarget.class);
        adapters.forEach(adapter -> {
            if (this.adapters.put(adapter.target(), adapter) != null) {
                throw new IllegalStateException("duplicate control-plane adapter for " + adapter.target());
            }
        });
        this.properties = properties;
        this.clock = clock;
        this.owner = instanceId(properties);
    }

    @Scheduled(fixedDelayString = "${emall.control-plane.reconcile-delay:1s}")
    public void reconcileScheduled() {
        reconcileBatch();
    }

    public int reconcileBatch() {
        Instant now = clock.instant();
        List<ControlPlaneOperation> candidates = store.findClaimable(now, properties.getBatchSize());
        int claimed = 0;
        for (ControlPlaneOperation candidate : candidates) {
            if (store.claim(candidate.operationId(), owner, now.plus(properties.getLeaseDuration()), now)) {
                claimed++;
                reconcileClaimed(candidate.operationId());
            }
        }
        return claimed;
    }

    public Optional<ControlPlaneOperation> reconcileNow(String operationId) {
        Instant now = clock.instant();
        if (store.claim(operationId, owner, now.plus(properties.getLeaseDuration()), now)) {
            reconcileClaimed(operationId);
        }
        return store.find(operationId);
    }

    private void reconcileClaimed(String operationId) {
        ControlPlaneOperation operation = store.find(operationId).orElseThrow();
        ControlPlaneAdapter adapter = adapters.get(operation.target());
        if (adapter == null) {
            retryOrFail(operation, "no adapter configured for " + operation.target(), null);
            return;
        }
        try {
            operation = captureRollbackState(operation, adapter);
            switch (operation.status()) {
                case PENDING, RETRYING -> apply(operation, adapter, false);
                case APPLYING -> apply(operation, adapter, true);
                case VERIFYING -> verify(operation, adapter);
                case ROLLING_BACK -> rollback(operation, adapter);
                case SUCCEEDED, ROLLED_BACK, FAILED -> releaseTerminalLease(operation);
            }
        } catch (RuntimeException exception) {
            retryOrFail(operation, exceptionMessage(exception), exception);
        }
    }

    private ControlPlaneOperation captureRollbackState(ControlPlaneOperation operation, ControlPlaneAdapter adapter) {
        if (operation.rollbackState() != null) {
            return operation;
        }
        Map<String, Object> rollbackState = adapter.captureRollbackState(operation);
        if (!store.saveRollbackState(operation.operationId(), owner, rollbackState, clock.instant())) {
            throw new IllegalStateException("control-plane lease was lost while saving rollback state");
        }
        return store.find(operation.operationId()).orElseThrow();
    }

    private void apply(ControlPlaneOperation operation, ControlPlaneAdapter adapter, boolean recovering) {
        if (recovering) {
            ControlPlaneObservation observation = adapter.observe(operation);
            if (observation.converged()) {
                complete(operation, observation);
                return;
            }
        }
        int attempt = operation.attemptCount() + 1;
        if (!transition(operation, ControlPlaneOperationStatus.APPLYING, attempt, operation.observedState(), null,
                clock.instant(), false)) {
            return;
        }
        operation = store.find(operation.operationId()).orElseThrow();
        adapter.apply(operation);
        ControlPlaneObservation observation = adapter.observe(operation);
        if (observation.converged()) {
            complete(operation, observation);
        } else if (attempt >= operation.maxAttempts()) {
            rollback(operation, adapter);
        } else {
            transition(operation, ControlPlaneOperationStatus.VERIFYING, attempt, observation.state(),
                    observation.detail(), nextAttempt(attempt), true);
        }
    }

    private void verify(ControlPlaneOperation operation, ControlPlaneAdapter adapter) {
        int attempt = operation.attemptCount() + 1;
        ControlPlaneObservation observation = adapter.observe(operation);
        if (observation.converged()) {
            complete(operation, observation);
        } else if (attempt >= operation.maxAttempts()) {
            rollback(operation, adapter);
        } else {
            transition(operation, ControlPlaneOperationStatus.VERIFYING, attempt, observation.state(),
                    observation.detail(), nextAttempt(attempt), true);
        }
    }

    private void rollback(ControlPlaneOperation operation, ControlPlaneAdapter adapter) {
        int attempt = operation.attemptCount() + 1;
        transition(operation, ControlPlaneOperationStatus.ROLLING_BACK, attempt, operation.observedState(),
                operation.lastError(), clock.instant(), false);
        operation = store.find(operation.operationId()).orElseThrow();
        try {
            adapter.rollback(operation);
            ControlPlaneObservation observation = adapter.observeRollback(operation);
            if (observation.converged()) {
                transition(operation, ControlPlaneOperationStatus.ROLLED_BACK, attempt, observation.state(),
                        operation.lastError(), clock.instant(), true);
            } else {
                continueRollback(operation, attempt, observation.state(), observation.detail());
            }
        } catch (RuntimeException exception) {
            continueRollback(operation, attempt, operation.observedState(), exceptionMessage(exception));
        }
    }

    private void continueRollback(ControlPlaneOperation operation, int attempt, Map<String, Object> observedState,
            String error) {
        int limit = operation.maxAttempts() + properties.getRollbackAttempts();
        ControlPlaneOperationStatus status =
                attempt >= limit ? ControlPlaneOperationStatus.FAILED : ControlPlaneOperationStatus.ROLLING_BACK;
        transition(operation, status, attempt, observedState, error,
                status == ControlPlaneOperationStatus.FAILED ? clock.instant() : nextAttempt(attempt), true);
    }

    private void complete(ControlPlaneOperation operation, ControlPlaneObservation observation) {
        transition(operation, ControlPlaneOperationStatus.SUCCEEDED, operation.attemptCount(), observation.state(),
                null, clock.instant(), true);
    }

    private void retryOrFail(ControlPlaneOperation operation, String error, RuntimeException exception) {
        int attempt = operation.attemptCount() + 1;
        if (attempt >= operation.maxAttempts() && operation.rollbackState() != null) {
            ControlPlaneAdapter adapter = adapters.get(operation.target());
            if (adapter != null) {
                rollback(operation, adapter);
                return;
            }
        }
        ControlPlaneOperationStatus status = attempt >= operation.maxAttempts()
                ? ControlPlaneOperationStatus.FAILED
                : ControlPlaneOperationStatus.RETRYING;
        transition(operation, status, attempt, operation.observedState(), error,
                status == ControlPlaneOperationStatus.FAILED ? clock.instant() : nextAttempt(attempt), true);
        if (exception != null) {
            LOGGER.warn("Control-plane operation {} failed on attempt {}", operation.operationId(), attempt, exception);
        }
    }

    private void releaseTerminalLease(ControlPlaneOperation operation) {
        transition(operation, operation.status(), operation.attemptCount(), operation.observedState(),
                operation.lastError(), clock.instant(), true);
    }

    private boolean transition(ControlPlaneOperation operation, ControlPlaneOperationStatus status, int attempt,
            Map<String, Object> observedState, String error, Instant nextAttemptAt, boolean releaseLease) {
        return store.transition(operation.operationId(), owner, status, attempt, observedState, truncate(error),
                nextAttemptAt, releaseLease, clock.instant());
    }

    private Instant nextAttempt(int attempt) {
        int exponent = Math.min(Math.max(0, attempt - 1), 10);
        Duration delay = properties.getRetryBaseDelay().multipliedBy(1L << exponent);
        return clock.instant().plus(delay);
    }

    private String exceptionMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return truncate(exception.getClass().getSimpleName() + (message == null ? "" : ": " + message));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private static String instanceId(ControlPlaneProperties properties) {
        if (properties.getInstanceId() != null && !properties.getInstanceId().isBlank()) {
            return properties.getInstanceId();
        }
        return System.getenv().getOrDefault("HOSTNAME", "control-plane") + '-' + java.util.UUID.randomUUID();
    }
}
