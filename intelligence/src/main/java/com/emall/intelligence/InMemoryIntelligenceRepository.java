package com.emall.intelligence;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryIntelligenceRepository implements IntelligenceRepository {
    private final ConcurrentMap<Long, UserProfile> userProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ItemProfile> itemProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FeatureDefinition> features = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, OnlineFeatureValue> values = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ModelDeployment> models = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AiDecision> decisions = new ConcurrentHashMap<>();

    @Override
    public UserProfile saveUserProfile(UserProfile profile) {
        userProfiles.put(profile.profileId(), profile);
        return profile;
    }

    @Override
    public List<UserProfile> findUserProfiles() {
        return List.copyOf(userProfiles.values());
    }

    @Override
    public ItemProfile saveItemProfile(ItemProfile profile) {
        itemProfiles.put(profile.profileId(), profile);
        return profile;
    }

    @Override
    public List<ItemProfile> findItemProfiles() {
        return List.copyOf(itemProfiles.values());
    }

    @Override
    public FeatureDefinition saveFeature(FeatureDefinition feature) {
        features.put(feature.featureId(), feature);
        return feature;
    }

    @Override
    public List<FeatureDefinition> findFeatures() {
        return List.copyOf(features.values());
    }

    @Override
    public OnlineFeatureValue saveFeatureValue(OnlineFeatureValue value) {
        values.put(value.valueId(), value);
        return value;
    }

    @Override
    public ModelDeployment saveModel(ModelDeployment model) {
        models.put(model.modelId(), model);
        return model;
    }

    @Override
    public Optional<ModelDeployment> findModel(long modelId) {
        return Optional.ofNullable(models.get(modelId));
    }

    @Override
    public List<ModelDeployment> findModels() {
        return List.copyOf(models.values());
    }

    @Override
    public AiDecision saveDecision(AiDecision decision) {
        decisions.put(decision.decisionId(), decision);
        return decision;
    }

    @Override
    public List<AiDecision> findDecisions() {
        return List.copyOf(decisions.values());
    }
}
