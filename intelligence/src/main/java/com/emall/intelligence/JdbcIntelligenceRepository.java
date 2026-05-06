package com.emall.intelligence;

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
class JdbcIntelligenceRepository implements IntelligenceRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcIntelligenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserProfile saveUserProfile(UserProfile profile) {
        jdbcTemplate.update("""
                INSERT INTO user_profile
                    (profile_id, user_id, segment, preferences, privacy_restricted, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE segment = VALUES(segment), preferences = VALUES(preferences),
                    privacy_restricted = VALUES(privacy_restricted), updated_at = VALUES(updated_at)
                """, profile.profileId(), profile.userId(), profile.segment(), profile.preferences(),
                profile.privacyRestricted(), Timestamp.from(profile.updatedAt()));
        return profile;
    }

    @Override
    public List<UserProfile> findUserProfiles() {
        return jdbcTemplate.query("SELECT * FROM user_profile", this::mapUserProfile);
    }

    @Override
    public ItemProfile saveItemProfile(ItemProfile profile) {
        jdbcTemplate.update("""
                INSERT INTO item_profile
                    (profile_id, sku_id, category, attributes, quality_score, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE category = VALUES(category), attributes = VALUES(attributes),
                    quality_score = VALUES(quality_score), updated_at = VALUES(updated_at)
                """, profile.profileId(), profile.skuId(), profile.category(), profile.attributes(),
                profile.qualityScore(), Timestamp.from(profile.updatedAt()));
        return profile;
    }

    @Override
    public List<ItemProfile> findItemProfiles() {
        return jdbcTemplate.query("SELECT * FROM item_profile", this::mapItemProfile);
    }

    @Override
    public FeatureDefinition saveFeature(FeatureDefinition feature) {
        jdbcTemplate.update("""
                INSERT INTO feature_definition
                    (feature_id, feature_name, scope, owner, freshness_seconds, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, feature.featureId(), feature.featureName(), feature.scope().name(), feature.owner(),
                feature.freshnessSeconds(), Timestamp.from(feature.createdAt()));
        return feature;
    }

    @Override
    public List<FeatureDefinition> findFeatures() {
        return jdbcTemplate.query("SELECT * FROM feature_definition", this::mapFeature);
    }

    @Override
    public OnlineFeatureValue saveFeatureValue(OnlineFeatureValue value) {
        jdbcTemplate.update("""
                INSERT INTO online_feature_value
                    (value_id, feature_name, entity_key, feature_value, event_time, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, value.valueId(), value.featureName(), value.entityKey(), value.featureValue(),
                Timestamp.from(value.eventTime()), Timestamp.from(value.updatedAt()));
        return value;
    }

    @Override
    public ModelDeployment saveModel(ModelDeployment model) {
        jdbcTemplate.update("""
                INSERT INTO model_deployment
                    (model_id, model_name, version, use_case, status, approval_ticket, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), approval_ticket = VALUES(approval_ticket),
                    updated_at = VALUES(updated_at)
                """, model.modelId(), model.modelName(), model.version(), model.useCase(), model.status().name(),
                model.approvalTicket(), Timestamp.from(model.createdAt()), Timestamp.from(model.updatedAt()));
        return model;
    }

    @Override
    public Optional<ModelDeployment> findModel(long modelId) {
        return jdbcTemplate.query("SELECT * FROM model_deployment WHERE model_id = ?", this::mapModel, modelId)
                .stream().findFirst();
    }

    @Override
    public List<ModelDeployment> findModels() {
        return jdbcTemplate.query("SELECT * FROM model_deployment", this::mapModel);
    }

    @Override
    public AiDecision saveDecision(AiDecision decision) {
        jdbcTemplate.update("""
                INSERT INTO ai_decision
                    (decision_id, use_case, entity_key, decision, score, model_version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, decision.decisionId(), decision.useCase(), decision.entityKey(), decision.decision(),
                decision.score(), decision.modelVersion(), Timestamp.from(decision.createdAt()));
        return decision;
    }

    @Override
    public List<AiDecision> findDecisions() {
        return jdbcTemplate.query("SELECT * FROM ai_decision", this::mapDecision);
    }

    private UserProfile mapUserProfile(ResultSet rs, int rowNum) throws SQLException {
        return new UserProfile(rs.getLong("profile_id"), rs.getLong("user_id"), rs.getString("segment"),
                rs.getString("preferences"), rs.getBoolean("privacy_restricted"),
                rs.getTimestamp("updated_at").toInstant());
    }

    private ItemProfile mapItemProfile(ResultSet rs, int rowNum) throws SQLException {
        return new ItemProfile(rs.getLong("profile_id"), rs.getLong("sku_id"), rs.getString("category"),
                rs.getString("attributes"), rs.getBigDecimal("quality_score"),
                rs.getTimestamp("updated_at").toInstant());
    }

    private FeatureDefinition mapFeature(ResultSet rs, int rowNum) throws SQLException {
        return new FeatureDefinition(rs.getLong("feature_id"), rs.getString("feature_name"),
                FeatureScope.valueOf(rs.getString("scope")), rs.getString("owner"),
                rs.getInt("freshness_seconds"), rs.getTimestamp("created_at").toInstant());
    }

    private ModelDeployment mapModel(ResultSet rs, int rowNum) throws SQLException {
        return new ModelDeployment(rs.getLong("model_id"), rs.getString("model_name"), rs.getString("version"),
                rs.getString("use_case"), ModelStatus.valueOf(rs.getString("status")),
                rs.getString("approval_ticket"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private AiDecision mapDecision(ResultSet rs, int rowNum) throws SQLException {
        return new AiDecision(rs.getLong("decision_id"), rs.getString("use_case"), rs.getString("entity_key"),
                rs.getString("decision"), rs.getBigDecimal("score"), rs.getString("model_version"),
                rs.getTimestamp("created_at").toInstant());
    }
}
