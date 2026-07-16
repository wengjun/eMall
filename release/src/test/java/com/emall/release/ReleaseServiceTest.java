package com.emall.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emall.common.controlplane.ControlPlaneCommand;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneProperties;
import com.emall.common.controlplane.ControlPlaneTarget;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReleaseServiceTest {
    private final InMemoryReleaseRepository repository = new InMemoryReleaseRepository();
    private final ControlPlaneClient controlPlaneClient = mock(ControlPlaneClient.class);
    private final ReleaseControlPlanePublisher publisher =
            new ReleaseControlPlanePublisher(controlPlaneClient, new ControlPlaneProperties());
    private final ReleaseService service =
            new ReleaseService(repository, new SnowflakeIdGenerator(63L), publisher, controlPlaneClient);

    @Test
    void managesFeatureRolloutTopicAndReplayGovernance() {
        FeatureToggle toggle = service.createToggle("new-checkout", "order", ToggleStatus.OFF, 0);
        service.updateToggle(toggle.toggleId(), ToggleStatus.ON, 10);
        RolloutPlan rollout = service.createRollout("order", "2026.04.29", "canary", 5);
        service.evaluatePreTrafficGuard(rollout.rolloutId(), true, true, true, true);
        service.changeRollout(rollout.rolloutId(), RolloutStatus.RUNNING, 20);
        service.registerTopic("order.created", "order-team", "v1", 10000);
        ReplayPlan replay = service.createReplay("order.created", "search-indexer", 0, 1000);
        service.changeReplayStatus(replay.replayId(), RolloutStatus.RUNNING);

        ReleaseSummary summary = service.summary();

        assertThat(summary.enabledFlags()).isEqualTo(1);
        assertThat(summary.runningRollouts()).isEqualTo(1);
        assertThat(summary.activeTopics()).isEqualTo(1);
        assertThat(summary.openReplays()).isEqualTo(1);
        ArgumentCaptor<ControlPlaneCommand> commands = ArgumentCaptor.forClass(ControlPlaneCommand.class);
        verify(controlPlaneClient, atLeastOnce()).submit(commands.capture());
        assertThat(commands.getAllValues()).extracting(ControlPlaneCommand::target).contains(
                ControlPlaneTarget.NACOS_CONFIG, ControlPlaneTarget.KUBERNETES_RESOURCE,
                ControlPlaneTarget.KAFKA_CONSUMER_OFFSETS);
        assertThatThrownBy(() -> service.changeReplayStatus(replay.replayId(), RolloutStatus.COMPLETED))
                .isInstanceOf(BusinessException.class).hasMessageContaining("has not been submitted");
    }

    @Test
    void blocksRolloutWithoutPreTrafficGuard() {
        RolloutPlan rollout = service.createRollout("payment", "2026.04.29", "blue-green", 0);

        assertThatThrownBy(() -> service.changeRollout(rollout.rolloutId(), RolloutStatus.RUNNING, 10))
                .isInstanceOf(BusinessException.class).hasMessageContaining("pre-traffic guard");
    }

    @Test
    void pausesRolloutWhenPreTrafficGuardFails() {
        RolloutPlan rollout = service.createRollout("inventory", "2026.04.29", "canary", 5);

        ReleaseGuardRecord guard = service.evaluatePreTrafficGuard(rollout.rolloutId(), true, true, false, true);

        assertThat(guard.decision()).isEqualTo(ReleaseGuardDecision.BLOCK);
        assertThat(repository.findRollout(rollout.rolloutId()).orElseThrow().status()).isEqualTo(RolloutStatus.PAUSED);
    }

    @Test
    void rollsBackCanaryAndRequiresRecoveryChecks() {
        RolloutPlan rollout = service.createRollout("order", "2026.04.30", "canary", 5);
        service.evaluatePreTrafficGuard(rollout.rolloutId(), true, true, true, true);

        ReleaseGuardRecord guard = service.evaluateCanaryGuard(rollout.rolloutId(), 20, new BigDecimal("0.040000"),
                1200, new BigDecimal("0.970000"));
        ReleaseGuardRecord recovery = service.verifyRollbackRecovery(rollout.rolloutId(), true, false, true);

        assertThat(guard.decision()).isEqualTo(ReleaseGuardDecision.ROLLBACK);
        assertThat(repository.findRollout(rollout.rolloutId()).orElseThrow().status())
                .isEqualTo(RolloutStatus.ROLLED_BACK);
        assertThat(recovery.decision()).isEqualTo(ReleaseGuardDecision.BLOCK);
        assertThat(recovery.messageReplayChecked()).isFalse();
    }
}
