package com.emall.common.controlplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ControlPlaneReconcilerTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-15T00:00:00Z"));
    private final InMemoryControlPlaneOperationStore store = new InMemoryControlPlaneOperationStore();
    private final ControlPlaneProperties properties = properties();
    private final ControlPlaneCommandService service =
            new ControlPlaneCommandService(store, properties, new ObjectMapper(), clock);

    @Test
    void appliesReadsBackAndCompletesOperation() {
        StatefulAdapter adapter = new StatefulAdapter(Map.of("value", "old"), false);
        ControlPlaneOperation submitted = service.submit(command("request-1", "new"));
        ControlPlaneReconciler reconciler = new ControlPlaneReconciler(store, List.of(adapter), properties, clock);

        assertThat(reconciler.reconcileBatch()).isEqualTo(1);

        ControlPlaneOperation completed = service.find(submitted.operationId()).orElseThrow();
        assertThat(completed.status()).isEqualTo(ControlPlaneOperationStatus.SUCCEEDED);
        assertThat(completed.rollbackState()).containsEntry("exists", true);
        assertThat(completed.observedState()).containsEntry("value", "new");
        assertThat(adapter.captureCount).isEqualTo(1);
        assertThat(adapter.applyCount).isEqualTo(1);
    }

    @Test
    void rollsBackPartialApplyAfterVerificationBudgetIsExhausted() {
        StatefulAdapter adapter = new StatefulAdapter(Map.of("value", "old"), true);
        ControlPlaneOperation submitted = service.submit(command("request-2", "new"));
        ControlPlaneReconciler reconciler = new ControlPlaneReconciler(store, List.of(adapter), properties, clock);

        reconciler.reconcileBatch();
        clock.advance(Duration.ofSeconds(1));
        reconciler.reconcileBatch();

        ControlPlaneOperation rolledBack = service.find(submitted.operationId()).orElseThrow();
        assertThat(rolledBack.status()).isEqualTo(ControlPlaneOperationStatus.ROLLED_BACK);
        assertThat(adapter.current).containsEntry("value", "old");
        assertThat(adapter.rollbackCount).isEqualTo(1);
    }

    @Test
    void expiredLeaseRecoversByObservingBeforeReapplying() {
        StatefulAdapter adapter = new StatefulAdapter(Map.of("value", "old"), false);
        ControlPlaneOperation submitted = service.submit(command("request-3", "new"));
        Instant now = clock.instant();
        assertThat(store.claim(submitted.operationId(), "dead-controller", now.plusSeconds(30), now)).isTrue();
        assertThat(store.saveRollbackState(submitted.operationId(), "dead-controller",
                Map.of("exists", true, "state", adapter.current), now)).isTrue();
        assertThat(store.transition(submitted.operationId(), "dead-controller", ControlPlaneOperationStatus.APPLYING, 1,
                null, null, now, false, now)).isTrue();
        adapter.current = submitted.desiredState();
        clock.advance(Duration.ofSeconds(31));

        ControlPlaneReconciler restarted = new ControlPlaneReconciler(store, List.of(adapter), properties, clock);
        restarted.reconcileBatch();

        assertThat(service.find(submitted.operationId()).orElseThrow().status())
                .isEqualTo(ControlPlaneOperationStatus.SUCCEEDED);
        assertThat(adapter.applyCount).isZero();
    }

    @Test
    void reusesIdempotencyKeyAndRejectsDifferentDesiredState() {
        ControlPlaneOperation first = service.submit(command("request-4", "new"));
        ControlPlaneOperation replay = service.submit(command("request-4", "new"));

        assertThat(replay.operationId()).isEqualTo(first.operationId());
        assertThatThrownBy(() -> service.submit(command("request-4", "different")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different desired state");
    }

    private ControlPlaneCommand command(String key, String value) {
        return ControlPlaneCommands.infrastructure(key, "release", "rollout", "deployment", "order",
                Map.of("value", value));
    }

    private ControlPlaneProperties properties() {
        ControlPlaneProperties configured = new ControlPlaneProperties();
        configured.setEnabled(true);
        configured.setInstanceId("test-controller");
        configured.setMaxAttempts(2);
        configured.setRollbackAttempts(2);
        configured.setLeaseDuration(Duration.ofSeconds(30));
        configured.setRetryBaseDelay(Duration.ZERO);
        return configured;
    }

    private static final class StatefulAdapter implements ControlPlaneAdapter {
        private Map<String, Object> current;
        private final boolean neverConverge;
        private int captureCount;
        private int applyCount;
        private int rollbackCount;

        private StatefulAdapter(Map<String, Object> initial, boolean neverConverge) {
            this.current = initial;
            this.neverConverge = neverConverge;
        }

        @Override
        public ControlPlaneTarget target() {
            return ControlPlaneTarget.INFRASTRUCTURE_API;
        }

        @Override
        public Map<String, Object> captureRollbackState(ControlPlaneOperation operation) {
            captureCount++;
            return Map.of("exists", true, "state", current);
        }

        @Override
        public void apply(ControlPlaneOperation operation) {
            applyCount++;
            current = neverConverge ? Map.of("value", "partial") : operation.desiredState();
        }

        @Override
        public ControlPlaneObservation observe(ControlPlaneOperation operation) {
            return new ControlPlaneObservation(current.equals(operation.desiredState()), current, "observed");
        }

        @Override
        public void rollback(ControlPlaneOperation operation) {
            rollbackCount++;
            current = ControlPlaneStateValues.map(operation.rollbackState(), "state");
        }

        @Override
        public ControlPlaneObservation observeRollback(ControlPlaneOperation operation) {
            Map<String, Object> expected = ControlPlaneStateValues.map(operation.rollbackState(), "state");
            return new ControlPlaneObservation(current.equals(expected), current, "rollback observed");
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
