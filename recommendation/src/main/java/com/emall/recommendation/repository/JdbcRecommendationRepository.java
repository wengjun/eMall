package com.emall.recommendation.repository;

import com.emall.recommendation.domain.BehaviorType;
import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ExperimentStatus;
import com.emall.recommendation.domain.ItemFeature;
import com.emall.recommendation.domain.UserBehaviorEvent;
import com.emall.recommendation.domain.UserPreference;
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
public class JdbcRecommendationRepository implements RecommendationRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcRecommendationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserPreference saveUserPreference(UserPreference preference) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_user_preference
                    (user_id, category_code, affinity_score, updated_at)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE affinity_score = VALUES(affinity_score), updated_at = VALUES(updated_at)
                """, preference.userId(), preference.categoryCode(), preference.affinityScore(),
                Timestamp.from(preference.updatedAt()));
        return preference;
    }

    @Override
    public Optional<UserPreference> findUserPreference(long userId, String categoryCode) {
        return jdbcTemplate.query("""
                SELECT * FROM recommendation_user_preference
                WHERE user_id = ? AND category_code = ?
                """, this::mapPreference, userId, categoryCode).stream().findFirst();
    }

    @Override
    public List<UserPreference> findUserPreferences(long userId) {
        return jdbcTemplate.query("""
                SELECT * FROM recommendation_user_preference
                WHERE user_id = ?
                ORDER BY affinity_score DESC, updated_at DESC
                """, this::mapPreference, userId);
    }

    @Override
    public ItemFeature saveItemFeature(ItemFeature feature) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_item_feature
                    (sku_id, category_code, base_score, popularity_score, active, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE category_code = VALUES(category_code), base_score = VALUES(base_score),
                    popularity_score = VALUES(popularity_score), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """, feature.skuId(), feature.categoryCode(), feature.baseScore(), feature.popularityScore(),
                feature.active(), Timestamp.from(feature.updatedAt()));
        return feature;
    }

    @Override
    public Optional<ItemFeature> findItemFeature(long skuId) {
        return jdbcTemplate
                .query("SELECT * FROM recommendation_item_feature WHERE sku_id = ?", this::mapItemFeature, skuId)
                .stream().findFirst();
    }

    @Override
    public List<ItemFeature> findActiveItemFeatures(int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM recommendation_item_feature
                WHERE active = TRUE
                ORDER BY popularity_score DESC, base_score DESC, sku_id ASC
                LIMIT ?
                """, this::mapItemFeature, limit);
    }

    @Override
    public UserBehaviorEvent saveBehaviorEvent(UserBehaviorEvent event) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_behavior_event
                    (event_id, user_id, sku_id, category_code, behavior_type, weight, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.userId(), event.skuId(), event.categoryCode(), event.behaviorType().name(),
                event.weight(), Timestamp.from(event.occurredAt()));
        return event;
    }

    @Override
    public Experiment saveExperiment(Experiment experiment) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_experiment
                    (experiment_id, scene, name, traffic_percent, control_strategy, treatment_strategy, status,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), traffic_percent = VALUES(traffic_percent),
                    control_strategy = VALUES(control_strategy), treatment_strategy = VALUES(treatment_strategy),
                    status = VALUES(status), updated_at = VALUES(updated_at)
                """, experiment.experimentId(), experiment.scene(), experiment.name(), experiment.trafficPercent(),
                experiment.controlStrategy(), experiment.treatmentStrategy(), experiment.status().name(),
                Timestamp.from(experiment.createdAt()), Timestamp.from(experiment.updatedAt()));
        return experiment;
    }

    @Override
    public Optional<Experiment> findExperiment(long experimentId) {
        return jdbcTemplate.query("SELECT * FROM recommendation_experiment WHERE experiment_id = ?",
                this::mapExperiment, experimentId).stream().findFirst();
    }

    @Override
    public Optional<Experiment> findActiveExperiment(String scene) {
        return jdbcTemplate.query("""
                SELECT * FROM recommendation_experiment
                WHERE scene = ? AND status = 'ACTIVE'
                ORDER BY updated_at DESC
                LIMIT 1
                """, this::mapExperiment, scene).stream().findFirst();
    }

    private UserPreference mapPreference(ResultSet rs, int rowNum) throws SQLException {
        return new UserPreference(rs.getLong("user_id"), rs.getString("category_code"), rs.getInt("affinity_score"),
                rs.getTimestamp("updated_at").toInstant());
    }

    private ItemFeature mapItemFeature(ResultSet rs, int rowNum) throws SQLException {
        return new ItemFeature(rs.getLong("sku_id"), rs.getString("category_code"), rs.getBigDecimal("base_score"),
                rs.getBigDecimal("popularity_score"), rs.getBoolean("active"),
                rs.getTimestamp("updated_at").toInstant());
    }

    private UserBehaviorEvent mapBehaviorEvent(ResultSet rs, int rowNum) throws SQLException {
        return new UserBehaviorEvent(rs.getLong("event_id"), rs.getLong("user_id"), rs.getLong("sku_id"),
                rs.getString("category_code"), BehaviorType.valueOf(rs.getString("behavior_type")), rs.getInt("weight"),
                rs.getTimestamp("occurred_at").toInstant());
    }

    private Experiment mapExperiment(ResultSet rs, int rowNum) throws SQLException {
        return new Experiment(rs.getLong("experiment_id"), rs.getString("scene"), rs.getString("name"),
                rs.getInt("traffic_percent"), rs.getString("control_strategy"), rs.getString("treatment_strategy"),
                ExperimentStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
