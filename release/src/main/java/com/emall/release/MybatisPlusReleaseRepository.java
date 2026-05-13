package com.emall.release;

import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(releaseMapper.findToggle(toggleId)).map(this::mapToggle);
    }

    @Override
    public List<FeatureToggle> findToggles() {
        return releaseMapper.findToggles().stream().map(this::mapToggle).toList();
    }

    @Override
    public RolloutPlan saveRollout(RolloutPlan rollout) {
        releaseMapper.saveRollout(rollout);
        return rollout;
    }

    @Override
    public Optional<RolloutPlan> findRollout(long rolloutId) {
        return Optional.ofNullable(releaseMapper.findRollout(rolloutId)).map(this::mapRollout);
    }

    @Override
    public List<RolloutPlan> findRollouts() {
        return releaseMapper.findRollouts().stream().map(this::mapRollout).toList();
    }

    @Override
    public MessageTopicGovernance saveTopic(MessageTopicGovernance topic) {
        releaseMapper.saveTopic(topic);
        return topic;
    }

    @Override
    public Optional<MessageTopicGovernance> findTopic(long topicId) {
        return Optional.ofNullable(releaseMapper.findTopic(topicId)).map(this::mapTopic);
    }

    @Override
    public List<MessageTopicGovernance> findTopics() {
        return releaseMapper.findTopics().stream().map(this::mapTopic).toList();
    }

    @Override
    public ReplayPlan saveReplay(ReplayPlan replay) {
        releaseMapper.saveReplay(replay);
        return replay;
    }

    @Override
    public Optional<ReplayPlan> findReplay(long replayId) {
        return Optional.ofNullable(releaseMapper.findReplay(replayId)).map(this::mapReplay);
    }

    @Override
    public List<ReplayPlan> findReplays() {
        return releaseMapper.findReplays().stream().map(this::mapReplay).toList();
    }

    private FeatureToggle mapToggle(Map<String, Object> row) {
        return new FeatureToggle(longValue(row, "toggle_id"), stringValue(row, "flag_key"),
                stringValue(row, "service_name"), ToggleStatus.valueOf(stringValue(row, "status")),
                intValue(row, "rollout_percent"), instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private RolloutPlan mapRollout(Map<String, Object> row) {
        return new RolloutPlan(longValue(row, "rollout_id"), stringValue(row, "service_name"),
                stringValue(row, "version"), stringValue(row, "strategy"), intValue(row, "current_percent"),
                RolloutStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private MessageTopicGovernance mapTopic(Map<String, Object> row) {
        return new MessageTopicGovernance(longValue(row, "topic_id"), stringValue(row, "topic_name"),
                stringValue(row, "owner"), stringValue(row, "schema_version"), longValue(row, "lag_budget"),
                TopicStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private ReplayPlan mapReplay(Map<String, Object> row) {
        return new ReplayPlan(longValue(row, "replay_id"), stringValue(row, "topic_name"),
                stringValue(row, "consumer_group"), longValue(row, "from_offset"), longValue(row, "to_offset"),
                RolloutStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }
}
