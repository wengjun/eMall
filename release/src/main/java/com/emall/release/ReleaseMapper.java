package com.emall.release;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface ReleaseMapper {
    @Insert("""
            INSERT INTO feature_toggle
                (toggle_id, flag_key, service_name, status, rollout_percent, created_at, updated_at)
            VALUES (#{toggle.toggleId}, #{toggle.flagKey}, #{toggle.serviceName}, #{toggle.status},
                #{toggle.rolloutPercent}, #{toggle.createdAt}, #{toggle.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), rollout_percent = VALUES(rollout_percent),
                updated_at = VALUES(updated_at)
            """)
    int saveToggle(@Param("toggle") FeatureToggle toggle);

    @Select("""
            SELECT toggle_id, flag_key, service_name, status, rollout_percent, created_at, updated_at
            FROM feature_toggle
            WHERE toggle_id = #{toggleId}
            """)
    FeatureToggle findToggle(@Param("toggleId") long toggleId);

    @Select("""
            SELECT toggle_id, flag_key, service_name, status, rollout_percent, created_at, updated_at
            FROM feature_toggle
            """)
    List<FeatureToggle> findToggles();

    @Insert("""
            INSERT INTO rollout_plan
                (rollout_id, service_name, version, strategy, current_percent, status, created_at, updated_at)
            VALUES (#{rollout.rolloutId}, #{rollout.serviceName}, #{rollout.version}, #{rollout.strategy},
                #{rollout.currentPercent}, #{rollout.status}, #{rollout.createdAt}, #{rollout.updatedAt})
            ON DUPLICATE KEY UPDATE current_percent = VALUES(current_percent), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveRollout(@Param("rollout") RolloutPlan rollout);

    @Select("""
            SELECT rollout_id, service_name, version, strategy, current_percent, status, created_at, updated_at
            FROM rollout_plan
            WHERE rollout_id = #{rolloutId}
            """)
    RolloutPlan findRollout(@Param("rolloutId") long rolloutId);

    @Select("""
            SELECT rollout_id, service_name, version, strategy, current_percent, status, created_at, updated_at
            FROM rollout_plan
            """)
    List<RolloutPlan> findRollouts();

    @Insert("""
            INSERT INTO message_topic_governance
                (topic_id, topic_name, owner, schema_version, lag_budget, status, created_at, updated_at)
            VALUES (#{topic.topicId}, #{topic.topicName}, #{topic.owner}, #{topic.schemaVersion},
                #{topic.lagBudget}, #{topic.status}, #{topic.createdAt}, #{topic.updatedAt})
            ON DUPLICATE KEY UPDATE owner = VALUES(owner), schema_version = VALUES(schema_version),
                lag_budget = VALUES(lag_budget), status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveTopic(@Param("topic") MessageTopicGovernance topic);

    @Select("""
            SELECT topic_id, topic_name, owner, schema_version, lag_budget, status, created_at, updated_at
            FROM message_topic_governance
            WHERE topic_id = #{topicId}
            """)
    MessageTopicGovernance findTopic(@Param("topicId") long topicId);

    @Select("""
            SELECT topic_id, topic_name, owner, schema_version, lag_budget, status, created_at, updated_at
            FROM message_topic_governance
            """)
    List<MessageTopicGovernance> findTopics();

    @Insert("""
            INSERT INTO replay_plan
                (replay_id, topic_name, consumer_group, from_offset, to_offset, status, created_at, updated_at)
            VALUES (#{replay.replayId}, #{replay.topicName}, #{replay.consumerGroup}, #{replay.fromOffset},
                #{replay.toOffset}, #{replay.status}, #{replay.createdAt}, #{replay.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveReplay(@Param("replay") ReplayPlan replay);

    @Select("""
            SELECT replay_id, topic_name, consumer_group, from_offset, to_offset, status, created_at, updated_at
            FROM replay_plan
            WHERE replay_id = #{replayId}
            """)
    ReplayPlan findReplay(@Param("replayId") long replayId);

    @Select("""
            SELECT replay_id, topic_name, consumer_group, from_offset, to_offset, status, created_at, updated_at
            FROM replay_plan
            """)
    List<ReplayPlan> findReplays();
}
