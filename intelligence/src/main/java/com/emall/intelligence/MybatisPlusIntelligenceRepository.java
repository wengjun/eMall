package com.emall.intelligence;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.decimalValue;
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
class MybatisPlusIntelligenceRepository implements IntelligenceRepository {
    private final IntelligenceMapper intelligenceMapper;

    MybatisPlusIntelligenceRepository(IntelligenceMapper intelligenceMapper) {
        this.intelligenceMapper = intelligenceMapper;
    }

    @Override
    public UserProfile saveUserProfile(UserProfile profile) {
        intelligenceMapper.saveUserProfile(profile);
        return profile;
    }

    @Override
    public List<UserProfile> findUserProfiles() {
        return intelligenceMapper.findUserProfiles().stream().map(this::mapUserProfile).toList();
    }

    @Override
    public ItemProfile saveItemProfile(ItemProfile profile) {
        intelligenceMapper.saveItemProfile(profile);
        return profile;
    }

    @Override
    public List<ItemProfile> findItemProfiles() {
        return intelligenceMapper.findItemProfiles().stream().map(this::mapItemProfile).toList();
    }

    @Override
    public FeatureDefinition saveFeature(FeatureDefinition feature) {
        intelligenceMapper.saveFeature(feature);
        return feature;
    }

    @Override
    public List<FeatureDefinition> findFeatures() {
        return intelligenceMapper.findFeatures().stream().map(this::mapFeature).toList();
    }

    @Override
    public OnlineFeatureValue saveFeatureValue(OnlineFeatureValue value) {
        intelligenceMapper.saveFeatureValue(value);
        return value;
    }

    @Override
    public ModelDeployment saveModel(ModelDeployment model) {
        intelligenceMapper.saveModel(model);
        return model;
    }

    @Override
    public Optional<ModelDeployment> findModel(long modelId) {
        return Optional.ofNullable(intelligenceMapper.findModel(modelId)).map(this::mapModel);
    }

    @Override
    public List<ModelDeployment> findModels() {
        return intelligenceMapper.findModels().stream().map(this::mapModel).toList();
    }

    @Override
    public AiDecision saveDecision(AiDecision decision) {
        intelligenceMapper.saveDecision(decision);
        return decision;
    }

    @Override
    public List<AiDecision> findDecisions() {
        return intelligenceMapper.findDecisions().stream().map(this::mapDecision).toList();
    }

    private UserProfile mapUserProfile(Map<String, Object> row) {
        return new UserProfile(longValue(row, "profile_id"), longValue(row, "user_id"), stringValue(row, "segment"),
                stringValue(row, "preferences"), booleanValue(row, "privacy_restricted"),
                instantValue(row, "updated_at"));
    }

    private ItemProfile mapItemProfile(Map<String, Object> row) {
        return new ItemProfile(longValue(row, "profile_id"), longValue(row, "sku_id"), stringValue(row, "category"),
                stringValue(row, "attributes"), decimalValue(row, "quality_score"), instantValue(row, "updated_at"));
    }

    private FeatureDefinition mapFeature(Map<String, Object> row) {
        return new FeatureDefinition(longValue(row, "feature_id"), stringValue(row, "feature_name"),
                FeatureScope.valueOf(stringValue(row, "scope")), stringValue(row, "owner"),
                intValue(row, "freshness_seconds"), instantValue(row, "created_at"));
    }

    private ModelDeployment mapModel(Map<String, Object> row) {
        return new ModelDeployment(longValue(row, "model_id"), stringValue(row, "model_name"),
                stringValue(row, "version"), stringValue(row, "use_case"),
                ModelStatus.valueOf(stringValue(row, "status")), stringValue(row, "approval_ticket"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private AiDecision mapDecision(Map<String, Object> row) {
        return new AiDecision(longValue(row, "decision_id"), stringValue(row, "use_case"),
                stringValue(row, "entity_key"), stringValue(row, "decision"), decimalValue(row, "score"),
                stringValue(row, "model_version"), instantValue(row, "created_at"));
    }
}
