package com.emall.release;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

class ReleaseServiceTest {
    private final InMemoryReleaseRepository repository = new InMemoryReleaseRepository();
    private final ReleaseService service = new ReleaseService(repository, new SnowflakeIdGenerator(63L));

    @Test
    void managesFeatureRolloutTopicAndReplayGovernance() {
        FeatureToggle toggle = service.createToggle("new-checkout", "order", ToggleStatus.OFF, 0);
        service.updateToggle(toggle.toggleId(), ToggleStatus.ON, 10);
        RolloutPlan rollout = service.createRollout("order", "2026.04.29", "canary", 5);
        service.changeRollout(rollout.rolloutId(), RolloutStatus.RUNNING, 20);
        service.registerTopic("order.created", "order-team", "v1", 10000);
        ReplayPlan replay = service.createReplay("order.created", "search-indexer", 0, 1000);
        service.changeReplayStatus(replay.replayId(), RolloutStatus.RUNNING);

        ReleaseSummary summary = service.summary();

        assertThat(summary.enabledFlags()).isEqualTo(1);
        assertThat(summary.runningRollouts()).isEqualTo(1);
        assertThat(summary.activeTopics()).isEqualTo(1);
        assertThat(summary.openReplays()).isEqualTo(1);
    }
}
