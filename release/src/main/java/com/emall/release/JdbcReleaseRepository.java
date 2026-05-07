package com.emall.release;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcReleaseRepository implements ReleaseRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcReleaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FeatureToggle saveToggle(FeatureToggle toggle) {
        jdbcTemplate.update("""
                INSERT INTO feature_toggle
                    (toggle_id, flag_key, service_name, status, rollout_percent, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), rollout_percent = VALUES(rollout_percent),
                    updated_at = VALUES(updated_at)
                """, toggle.toggleId(), toggle.flagKey(), toggle.serviceName(), toggle.status().name(),
                toggle.rolloutPercent(), Timestamp.from(toggle.createdAt()), Timestamp.from(toggle.updatedAt()));
        return toggle;
    }

    @Override
    public Optional<FeatureToggle> findToggle(long toggleId) {
        return jdbcTemplate.query("SELECT * FROM feature_toggle WHERE toggle_id = ?", this::mapToggle, toggleId)
                .stream().findFirst();
    }

    @Override
    public List<FeatureToggle> findToggles() {
        return jdbcTemplate.query("SELECT * FROM feature_toggle", this::mapToggle);
    }

    @Override
    public RolloutPlan saveRollout(RolloutPlan rollout) {
        jdbcTemplate.update("""
                INSERT INTO rollout_plan
                    (rollout_id, service_name, version, strategy, current_percent, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE current_percent = VALUES(current_percent), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, rollout.rolloutId(), rollout.serviceName(), rollout.version(), rollout.strategy(),
                rollout.currentPercent(), rollout.status().name(), Timestamp.from(rollout.createdAt()),
                Timestamp.from(rollout.updatedAt()));
        return rollout;
    }

    @Override
    public Optional<RolloutPlan> findRollout(long rolloutId) {
        return jdbcTemplate.query("SELECT * FROM rollout_plan WHERE rollout_id = ?", this::mapRollout, rolloutId)
                .stream().findFirst();
    }

    @Override
    public List<RolloutPlan> findRollouts() {
        return jdbcTemplate.query("SELECT * FROM rollout_plan", this::mapRollout);
    }

    @Override
    public MessageTopicGovernance saveTopic(MessageTopicGovernance topic) {
        jdbcTemplate.update("""
                INSERT INTO message_topic_governance
                    (topic_id, topic_name, owner, schema_version, lag_budget, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE owner = VALUES(owner), schema_version = VALUES(schema_version),
                    lag_budget = VALUES(lag_budget), status = VALUES(status), updated_at = VALUES(updated_at)
                """, topic.topicId(), topic.topicName(), topic.owner(), topic.schemaVersion(), topic.lagBudget(),
                topic.status().name(), Timestamp.from(topic.createdAt()), Timestamp.from(topic.updatedAt()));
        return topic;
    }

    @Override
    public Optional<MessageTopicGovernance> findTopic(long topicId) {
        return jdbcTemplate.query("SELECT * FROM message_topic_governance WHERE topic_id = ?", this::mapTopic, topicId)
                .stream().findFirst();
    }

    @Override
    public List<MessageTopicGovernance> findTopics() {
        return jdbcTemplate.query("SELECT * FROM message_topic_governance", this::mapTopic);
    }

    @Override
    public ReplayPlan saveReplay(ReplayPlan replay) {
        jdbcTemplate.update("""
                INSERT INTO replay_plan
                    (replay_id, topic_name, consumer_group, from_offset, to_offset, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, replay.replayId(), replay.topicName(), replay.consumerGroup(), replay.fromOffset(),
                replay.toOffset(), replay.status().name(), Timestamp.from(replay.createdAt()),
                Timestamp.from(replay.updatedAt()));
        return replay;
    }

    @Override
    public Optional<ReplayPlan> findReplay(long replayId) {
        return jdbcTemplate.query("SELECT * FROM replay_plan WHERE replay_id = ?", this::mapReplay, replayId).stream()
                .findFirst();
    }

    @Override
    public List<ReplayPlan> findReplays() {
        return jdbcTemplate.query("SELECT * FROM replay_plan", this::mapReplay);
    }

    private FeatureToggle mapToggle(ResultSet rs, int rowNum) throws SQLException {
        return new FeatureToggle(rs.getLong("toggle_id"), rs.getString("flag_key"), rs.getString("service_name"),
                ToggleStatus.valueOf(rs.getString("status")), rs.getInt("rollout_percent"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private RolloutPlan mapRollout(ResultSet rs, int rowNum) throws SQLException {
        return new RolloutPlan(rs.getLong("rollout_id"), rs.getString("service_name"), rs.getString("version"),
                rs.getString("strategy"), rs.getInt("current_percent"), RolloutStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private MessageTopicGovernance mapTopic(ResultSet rs, int rowNum) throws SQLException {
        return new MessageTopicGovernance(rs.getLong("topic_id"), rs.getString("topic_name"), rs.getString("owner"),
                rs.getString("schema_version"), rs.getLong("lag_budget"), TopicStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private ReplayPlan mapReplay(ResultSet rs, int rowNum) throws SQLException {
        return new ReplayPlan(rs.getLong("replay_id"), rs.getString("topic_name"), rs.getString("consumer_group"),
                rs.getLong("from_offset"), rs.getLong("to_offset"), RolloutStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
