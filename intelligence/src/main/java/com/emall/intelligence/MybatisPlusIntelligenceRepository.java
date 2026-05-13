package com.emall.intelligence;

import java.util.List;
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
        return intelligenceMapper.findUserProfiles();
    }

    @Override
    public ItemProfile saveItemProfile(ItemProfile profile) {
        intelligenceMapper.saveItemProfile(profile);
        return profile;
    }

    @Override
    public List<ItemProfile> findItemProfiles() {
        return intelligenceMapper.findItemProfiles();
    }

    @Override
    public FeatureDefinition saveFeature(FeatureDefinition feature) {
        intelligenceMapper.saveFeature(feature);
        return feature;
    }

    @Override
    public List<FeatureDefinition> findFeatures() {
        return intelligenceMapper.findFeatures();
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
        return Optional.ofNullable(intelligenceMapper.findModel(modelId));
    }

    @Override
    public List<ModelDeployment> findModels() {
        return intelligenceMapper.findModels();
    }

    @Override
    public AiDecision saveDecision(AiDecision decision) {
        intelligenceMapper.saveDecision(decision);
        return decision;
    }

    @Override
    public List<AiDecision> findDecisions() {
        return intelligenceMapper.findDecisions();
    }
}
