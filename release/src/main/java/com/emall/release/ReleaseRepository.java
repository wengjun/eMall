package com.emall.release;

import java.util.List;
import java.util.Optional;

interface ReleaseRepository {
    FeatureToggle saveToggle(FeatureToggle toggle);

    Optional<FeatureToggle> findToggle(long toggleId);

    List<FeatureToggle> findToggles();

    RolloutPlan saveRollout(RolloutPlan rollout);

    Optional<RolloutPlan> findRollout(long rolloutId);

    List<RolloutPlan> findRollouts();

    MessageTopicGovernance saveTopic(MessageTopicGovernance topic);

    Optional<MessageTopicGovernance> findTopic(long topicId);

    List<MessageTopicGovernance> findTopics();

    ReplayPlan saveReplay(ReplayPlan replay);

    Optional<ReplayPlan> findReplay(long replayId);

    List<ReplayPlan> findReplays();

    ReleaseGuardRecord saveGuard(ReleaseGuardRecord guard);

    List<ReleaseGuardRecord> findGuards(long rolloutId);

    List<ReleaseGuardRecord> findGuards();
}
