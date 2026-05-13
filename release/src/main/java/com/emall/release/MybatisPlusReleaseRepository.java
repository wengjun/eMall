package com.emall.release;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusReleaseRepository implements ReleaseRepository {
    private final ReleaseMapper releaseMapper;

    MybatisPlusReleaseRepository(ReleaseMapper releaseMapper) {
        this.releaseMapper = releaseMapper;
    }

    @Override
    public FeatureToggle saveToggle(FeatureToggle toggle) {
        releaseMapper.saveToggle(toggle);
        return toggle;
    }

    @Override
    public Optional<FeatureToggle> findToggle(long toggleId) {
        return Optional.ofNullable(releaseMapper.findToggle(toggleId));
    }

    @Override
    public List<FeatureToggle> findToggles() {
        return releaseMapper.findToggles();
    }

    @Override
    public RolloutPlan saveRollout(RolloutPlan rollout) {
        releaseMapper.saveRollout(rollout);
        return rollout;
    }

    @Override
    public Optional<RolloutPlan> findRollout(long rolloutId) {
        return Optional.ofNullable(releaseMapper.findRollout(rolloutId));
    }

    @Override
    public List<RolloutPlan> findRollouts() {
        return releaseMapper.findRollouts();
    }

    @Override
    public MessageTopicGovernance saveTopic(MessageTopicGovernance topic) {
        releaseMapper.saveTopic(topic);
        return topic;
    }

    @Override
    public Optional<MessageTopicGovernance> findTopic(long topicId) {
        return Optional.ofNullable(releaseMapper.findTopic(topicId));
    }

    @Override
    public List<MessageTopicGovernance> findTopics() {
        return releaseMapper.findTopics();
    }

    @Override
    public ReplayPlan saveReplay(ReplayPlan replay) {
        releaseMapper.saveReplay(replay);
        return replay;
    }

    @Override
    public Optional<ReplayPlan> findReplay(long replayId) {
        return Optional.ofNullable(releaseMapper.findReplay(replayId));
    }

    @Override
    public List<ReplayPlan> findReplays() {
        return releaseMapper.findReplays();
    }
}
