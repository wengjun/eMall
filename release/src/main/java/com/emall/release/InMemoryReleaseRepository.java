package com.emall.release;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryReleaseRepository implements ReleaseRepository {
    private final ConcurrentMap<Long, FeatureToggle> toggles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RolloutPlan> rollouts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MessageTopicGovernance> topics = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ReplayPlan> replays = new ConcurrentHashMap<>();

    @Override
    public FeatureToggle saveToggle(FeatureToggle toggle) {
        toggles.put(toggle.toggleId(), toggle);
        return toggle;
    }

    @Override
    public Optional<FeatureToggle> findToggle(long toggleId) {
        return Optional.ofNullable(toggles.get(toggleId));
    }

    @Override
    public List<FeatureToggle> findToggles() {
        return List.copyOf(toggles.values());
    }

    @Override
    public RolloutPlan saveRollout(RolloutPlan rollout) {
        rollouts.put(rollout.rolloutId(), rollout);
        return rollout;
    }

    @Override
    public Optional<RolloutPlan> findRollout(long rolloutId) {
        return Optional.ofNullable(rollouts.get(rolloutId));
    }

    @Override
    public List<RolloutPlan> findRollouts() {
        return List.copyOf(rollouts.values());
    }

    @Override
    public MessageTopicGovernance saveTopic(MessageTopicGovernance topic) {
        topics.put(topic.topicId(), topic);
        return topic;
    }

    @Override
    public Optional<MessageTopicGovernance> findTopic(long topicId) {
        return Optional.ofNullable(topics.get(topicId));
    }

    @Override
    public List<MessageTopicGovernance> findTopics() {
        return List.copyOf(topics.values());
    }

    @Override
    public ReplayPlan saveReplay(ReplayPlan replay) {
        replays.put(replay.replayId(), replay);
        return replay;
    }

    @Override
    public Optional<ReplayPlan> findReplay(long replayId) {
        return Optional.ofNullable(replays.get(replayId));
    }

    @Override
    public List<ReplayPlan> findReplays() {
        return List.copyOf(replays.values());
    }
}
