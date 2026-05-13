package com.emall.recommendation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.recommendation.domain.BehaviorType;
import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ExperimentStatus;
import com.emall.recommendation.domain.ItemFeature;
import com.emall.recommendation.domain.UserBehaviorEvent;
import com.emall.recommendation.domain.UserPreference;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusRecommendationRepository implements RecommendationRepository {
    private final UserPreferenceMapper preferenceMapper;
    private final ItemFeatureMapper itemFeatureMapper;
    private final UserBehaviorEventMapper behaviorEventMapper;
    private final RecommendationExperimentMapper experimentMapper;

    public MybatisPlusRecommendationRepository(UserPreferenceMapper preferenceMapper,
            ItemFeatureMapper itemFeatureMapper, UserBehaviorEventMapper behaviorEventMapper,
            RecommendationExperimentMapper experimentMapper) {
        this.preferenceMapper = preferenceMapper;
        this.itemFeatureMapper = itemFeatureMapper;
        this.behaviorEventMapper = behaviorEventMapper;
        this.experimentMapper = experimentMapper;
    }

    @Override
    public UserPreference saveUserPreference(UserPreference preference) {
        UserPreferenceEntity entity = toEntity(preference);
        try {
            preferenceMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            preferenceMapper.update(null, new UpdateWrapper<UserPreferenceEntity>()
                    .set("affinity_score", entity.getAffinityScore())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("user_id", entity.getUserId())
                    .eq("category_code", entity.getCategoryCode()));
        }
        return preference;
    }

    @Override
    public Optional<UserPreference> findUserPreference(long userId, String categoryCode) {
        return Optional.ofNullable(preferenceMapper.selectOne(new QueryWrapper<UserPreferenceEntity>()
                .eq("user_id", userId)
                .eq("category_code", categoryCode))).map(this::toDomain);
    }

    @Override
    public List<UserPreference> findUserPreferences(long userId) {
        return preferenceMapper.selectList(new QueryWrapper<UserPreferenceEntity>()
                .eq("user_id", userId)
                .orderByDesc("affinity_score", "updated_at")).stream().map(this::toDomain).toList();
    }

    @Override
    public ItemFeature saveItemFeature(ItemFeature feature) {
        ItemFeatureEntity entity = toEntity(feature);
        try {
            itemFeatureMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            itemFeatureMapper.update(null, new UpdateWrapper<ItemFeatureEntity>()
                    .set("category_code", entity.getCategoryCode())
                    .set("base_score", entity.getBaseScore())
                    .set("popularity_score", entity.getPopularityScore())
                    .set("active", entity.getActive())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("sku_id", entity.getSkuId()));
        }
        return feature;
    }

    @Override
    public Optional<ItemFeature> findItemFeature(long skuId) {
        return Optional.ofNullable(itemFeatureMapper.selectById(skuId)).map(this::toDomain);
    }

    @Override
    public List<ItemFeature> findActiveItemFeatures(int limit) {
        return itemFeatureMapper.selectList(new QueryWrapper<ItemFeatureEntity>()
                .eq("active", true)
                .orderByDesc("popularity_score", "base_score")
                .orderByAsc("sku_id")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    @Override
    public UserBehaviorEvent saveBehaviorEvent(UserBehaviorEvent event) {
        behaviorEventMapper.insert(toEntity(event));
        return event;
    }

    @Override
    public Experiment saveExperiment(Experiment experiment) {
        RecommendationExperimentEntity entity = toEntity(experiment);
        try {
            experimentMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            experimentMapper.update(null, new UpdateWrapper<RecommendationExperimentEntity>()
                    .set("name", entity.getName())
                    .set("traffic_percent", entity.getTrafficPercent())
                    .set("control_strategy", entity.getControlStrategy())
                    .set("treatment_strategy", entity.getTreatmentStrategy())
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("experiment_id", entity.getExperimentId()));
        }
        return experiment;
    }

    @Override
    public Optional<Experiment> findExperiment(long experimentId) {
        return Optional.ofNullable(experimentMapper.selectById(experimentId)).map(this::toDomain);
    }

    @Override
    public Optional<Experiment> findActiveExperiment(String scene) {
        return Optional.ofNullable(experimentMapper.selectOne(new QueryWrapper<RecommendationExperimentEntity>()
                .eq("scene", scene)
                .eq("status", ExperimentStatus.ACTIVE.name())
                .orderByDesc("updated_at")
                .last("LIMIT 1"))).map(this::toDomain);
    }

    private UserPreferenceEntity toEntity(UserPreference preference) {
        UserPreferenceEntity entity = new UserPreferenceEntity();
        entity.setUserId(preference.userId());
        entity.setCategoryCode(preference.categoryCode());
        entity.setAffinityScore(preference.affinityScore());
        entity.setUpdatedAt(LocalDateTime.ofInstant(preference.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private UserPreference toDomain(UserPreferenceEntity entity) {
        return new UserPreference(entity.getUserId(), entity.getCategoryCode(), entity.getAffinityScore(),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private ItemFeatureEntity toEntity(ItemFeature feature) {
        ItemFeatureEntity entity = new ItemFeatureEntity();
        entity.setSkuId(feature.skuId());
        entity.setCategoryCode(feature.categoryCode());
        entity.setBaseScore(feature.baseScore());
        entity.setPopularityScore(feature.popularityScore());
        entity.setActive(feature.active());
        entity.setUpdatedAt(LocalDateTime.ofInstant(feature.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private ItemFeature toDomain(ItemFeatureEntity entity) {
        return new ItemFeature(entity.getSkuId(), entity.getCategoryCode(), entity.getBaseScore(),
                entity.getPopularityScore(), entity.getActive(), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private UserBehaviorEventEntity toEntity(UserBehaviorEvent event) {
        UserBehaviorEventEntity entity = new UserBehaviorEventEntity();
        entity.setEventId(event.eventId());
        entity.setUserId(event.userId());
        entity.setSkuId(event.skuId());
        entity.setCategoryCode(event.categoryCode());
        entity.setBehaviorType(event.behaviorType().name());
        entity.setWeight(event.weight());
        entity.setOccurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
        return entity;
    }

    private UserBehaviorEvent toDomain(UserBehaviorEventEntity entity) {
        return new UserBehaviorEvent(entity.getEventId(), entity.getUserId(), entity.getSkuId(),
                entity.getCategoryCode(), BehaviorType.valueOf(entity.getBehaviorType()), entity.getWeight(),
                entity.getOccurredAt().toInstant(ZoneOffset.UTC));
    }

    private RecommendationExperimentEntity toEntity(Experiment experiment) {
        RecommendationExperimentEntity entity = new RecommendationExperimentEntity();
        entity.setExperimentId(experiment.experimentId());
        entity.setScene(experiment.scene());
        entity.setName(experiment.name());
        entity.setTrafficPercent(experiment.trafficPercent());
        entity.setControlStrategy(experiment.controlStrategy());
        entity.setTreatmentStrategy(experiment.treatmentStrategy());
        entity.setStatus(experiment.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(experiment.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(experiment.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Experiment toDomain(RecommendationExperimentEntity entity) {
        return new Experiment(entity.getExperimentId(), entity.getScene(), entity.getName(),
                entity.getTrafficPercent(), entity.getControlStrategy(), entity.getTreatmentStrategy(),
                ExperimentStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
