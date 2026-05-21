package com.emall.intelligence;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusIntelligenceRepository implements IntelligenceRepository {
    private final IntelligenceMapper intelligenceMapper;
    private final UserProfileMapper userProfileMapper;
    private final ItemProfileMapper itemProfileMapper;
    private final FeatureDefinitionMapper featureMapper;
    private final OnlineFeatureValueMapper featureValueMapper;
    private final ModelDeploymentMapper modelMapper;
    private final AiDecisionMapper decisionMapper;

    MybatisPlusIntelligenceRepository(IntelligenceMapper intelligenceMapper, UserProfileMapper userProfileMapper,
            ItemProfileMapper itemProfileMapper, FeatureDefinitionMapper featureMapper,
            OnlineFeatureValueMapper featureValueMapper, ModelDeploymentMapper modelMapper,
            AiDecisionMapper decisionMapper) {
        this.intelligenceMapper = intelligenceMapper;
        this.userProfileMapper = userProfileMapper;
        this.itemProfileMapper = itemProfileMapper;
        this.featureMapper = featureMapper;
        this.featureValueMapper = featureValueMapper;
        this.modelMapper = modelMapper;
        this.decisionMapper = decisionMapper;
    }

    @Override
    public UserProfile saveUserProfile(UserProfile profile) {
        intelligenceMapper.saveUserProfile(profile);
        return profile;
    }

    @Override
    public List<UserProfile> findUserProfiles() {
        return userProfileMapper.selectList(null);
    }

    @Override
    public ItemProfile saveItemProfile(ItemProfile profile) {
        intelligenceMapper.saveItemProfile(profile);
        return profile;
    }

    @Override
    public List<ItemProfile> findItemProfiles() {
        return itemProfileMapper.selectList(null);
    }

    @Override
    public FeatureDefinition saveFeature(FeatureDefinition feature) {
        featureMapper.insert(feature);
        return feature;
    }

    @Override
    public List<FeatureDefinition> findFeatures() {
        return featureMapper.selectList(null);
    }

    @Override
    public OnlineFeatureValue saveFeatureValue(OnlineFeatureValue value) {
        featureValueMapper.insert(value);
        return value;
    }

    @Override
    public ModelDeployment saveModel(ModelDeployment model) {
        intelligenceMapper.saveModel(model);
        return model;
    }

    @Override
    public Optional<ModelDeployment> findModel(long modelId) {
        return Optional.ofNullable(modelMapper.selectById(modelId));
    }

    @Override
    public List<ModelDeployment> findModels() {
        return modelMapper.selectList(null);
    }

    @Override
    public AiDecision saveDecision(AiDecision decision) {
        decisionMapper.insert(decision);
        return decision;
    }

    @Override
    public List<AiDecision> findDecisions() {
        return decisionMapper.selectList(null);
    }
}
