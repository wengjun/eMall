package com.emall.intelligence;

import java.util.List;
import java.util.Optional;

interface IntelligenceRepository {
    UserProfile saveUserProfile(UserProfile profile);

    List<UserProfile> findUserProfiles();

    ItemProfile saveItemProfile(ItemProfile profile);

    List<ItemProfile> findItemProfiles();

    FeatureDefinition saveFeature(FeatureDefinition feature);

    List<FeatureDefinition> findFeatures();

    OnlineFeatureValue saveFeatureValue(OnlineFeatureValue value);

    ModelDeployment saveModel(ModelDeployment model);

    Optional<ModelDeployment> findModel(long modelId);

    List<ModelDeployment> findModels();

    AiDecision saveDecision(AiDecision decision);

    List<AiDecision> findDecisions();
}
