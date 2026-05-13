package com.emall.intelligence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface IntelligenceMapper {
    @Insert("""
            INSERT INTO user_profile
                (profile_id, user_id, segment, preferences, privacy_restricted, updated_at)
            VALUES (#{profile.profileId}, #{profile.userId}, #{profile.segment}, #{profile.preferences},
                #{profile.privacyRestricted}, #{profile.updatedAt})
            ON DUPLICATE KEY UPDATE segment = VALUES(segment), preferences = VALUES(preferences),
                privacy_restricted = VALUES(privacy_restricted), updated_at = VALUES(updated_at)
            """)
    int saveUserProfile(@Param("profile") UserProfile profile);

    @Select("""
            SELECT profile_id, user_id, segment, preferences, privacy_restricted, updated_at
            FROM user_profile
            """)
    List<UserProfile> findUserProfiles();

    @Insert("""
            INSERT INTO item_profile
                (profile_id, sku_id, category, attributes, quality_score, updated_at)
            VALUES (#{profile.profileId}, #{profile.skuId}, #{profile.category}, #{profile.attributes},
                #{profile.qualityScore}, #{profile.updatedAt})
            ON DUPLICATE KEY UPDATE category = VALUES(category), attributes = VALUES(attributes),
                quality_score = VALUES(quality_score), updated_at = VALUES(updated_at)
            """)
    int saveItemProfile(@Param("profile") ItemProfile profile);

    @Select("""
            SELECT profile_id, sku_id, category, attributes, quality_score, updated_at
            FROM item_profile
            """)
    List<ItemProfile> findItemProfiles();

    @Insert("""
            INSERT INTO feature_definition
                (feature_id, feature_name, scope, owner, freshness_seconds, created_at)
            VALUES (#{feature.featureId}, #{feature.featureName}, #{feature.scope}, #{feature.owner},
                #{feature.freshnessSeconds}, #{feature.createdAt})
            """)
    int saveFeature(@Param("feature") FeatureDefinition feature);

    @Select("""
            SELECT feature_id, feature_name, scope, owner, freshness_seconds, created_at
            FROM feature_definition
            """)
    List<FeatureDefinition> findFeatures();

    @Insert("""
            INSERT INTO online_feature_value
                (value_id, feature_name, entity_key, feature_value, event_time, updated_at)
            VALUES (#{value.valueId}, #{value.featureName}, #{value.entityKey}, #{value.featureValue},
                #{value.eventTime}, #{value.updatedAt})
            """)
    int saveFeatureValue(@Param("value") OnlineFeatureValue value);

    @Insert("""
            INSERT INTO model_deployment
                (model_id, model_name, version, use_case, status, approval_ticket, created_at, updated_at)
            VALUES (#{model.modelId}, #{model.modelName}, #{model.version}, #{model.useCase}, #{model.status},
                #{model.approvalTicket}, #{model.createdAt}, #{model.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), approval_ticket = VALUES(approval_ticket),
                updated_at = VALUES(updated_at)
            """)
    int saveModel(@Param("model") ModelDeployment model);

    @Select("""
            SELECT model_id, model_name, version, use_case, status, approval_ticket, created_at, updated_at
            FROM model_deployment
            WHERE model_id = #{modelId}
            """)
    ModelDeployment findModel(@Param("modelId") long modelId);

    @Select("""
            SELECT model_id, model_name, version, use_case, status, approval_ticket, created_at, updated_at
            FROM model_deployment
            """)
    List<ModelDeployment> findModels();

    @Insert("""
            INSERT INTO ai_decision
                (decision_id, use_case, entity_key, decision, score, model_version, created_at)
            VALUES (#{decision.decisionId}, #{decision.useCase}, #{decision.entityKey}, #{decision.decision},
                #{decision.score}, #{decision.modelVersion}, #{decision.createdAt})
            """)
    int saveDecision(@Param("decision") AiDecision decision);

    @Select("""
            SELECT decision_id, use_case, entity_key, decision, score, model_version, created_at
            FROM ai_decision
            """)
    List<AiDecision> findDecisions();
}
