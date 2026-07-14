package com.emall.release;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.persistence.BoundedQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusReleaseRepository implements ReleaseRepository {
    private final ReleaseMapper releaseMapper;
    private final FeatureToggleMapper toggleMapper;
    private final RolloutPlanMapper rolloutMapper;
    private final MessageTopicGovernanceMapper topicMapper;
    private final ReplayPlanMapper replayMapper;
    private final ReleaseGuardRecordMapper guardMapper;

    MybatisPlusReleaseRepository(ReleaseMapper releaseMapper, FeatureToggleMapper toggleMapper,
            RolloutPlanMapper rolloutMapper, MessageTopicGovernanceMapper topicMapper, ReplayPlanMapper replayMapper,
            ReleaseGuardRecordMapper guardMapper) {
        this.releaseMapper = releaseMapper;
        this.toggleMapper = toggleMapper;
        this.rolloutMapper = rolloutMapper;
        this.topicMapper = topicMapper;
        this.replayMapper = replayMapper;
        this.guardMapper = guardMapper;
    }

    @Override
    public FeatureToggle saveToggle(FeatureToggle toggle) {
        releaseMapper.saveToggle(toggle);
        return toggle;
    }

    @Override
    public Optional<FeatureToggle> findToggle(long toggleId) {
        return Optional.ofNullable(toggleMapper.selectById(toggleId));
    }

    @Override
    public List<FeatureToggle> findToggles() {
        return BoundedQuery.firstPage(toggleMapper);
    }

    @Override
    public RolloutPlan saveRollout(RolloutPlan rollout) {
        releaseMapper.saveRollout(rollout);
        return rollout;
    }

    @Override
    public Optional<RolloutPlan> findRollout(long rolloutId) {
        return Optional.ofNullable(rolloutMapper.selectById(rolloutId));
    }

    @Override
    public List<RolloutPlan> findRollouts() {
        return BoundedQuery.firstPage(rolloutMapper);
    }

    @Override
    public MessageTopicGovernance saveTopic(MessageTopicGovernance topic) {
        releaseMapper.saveTopic(topic);
        return topic;
    }

    @Override
    public Optional<MessageTopicGovernance> findTopic(long topicId) {
        return Optional.ofNullable(topicMapper.selectById(topicId));
    }

    @Override
    public List<MessageTopicGovernance> findTopics() {
        return BoundedQuery.firstPage(topicMapper);
    }

    @Override
    public ReplayPlan saveReplay(ReplayPlan replay) {
        releaseMapper.saveReplay(replay);
        return replay;
    }

    @Override
    public Optional<ReplayPlan> findReplay(long replayId) {
        return Optional.ofNullable(replayMapper.selectById(replayId));
    }

    @Override
    public List<ReplayPlan> findReplays() {
        return BoundedQuery.firstPage(replayMapper);
    }

    @Override
    public ReleaseGuardRecord saveGuard(ReleaseGuardRecord guard) {
        guardMapper.insert(guard);
        return guard;
    }

    @Override
    public List<ReleaseGuardRecord> findGuards(long rolloutId) {
        return BoundedQuery.firstPage(guardMapper,
                new QueryWrapper<ReleaseGuardRecord>().eq("rollout_id", rolloutId).orderByDesc("created_at"));
    }

    @Override
    public List<ReleaseGuardRecord> findGuards() {
        return BoundedQuery.firstPage(guardMapper);
    }
}
