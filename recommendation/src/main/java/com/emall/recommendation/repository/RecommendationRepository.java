package com.emall.recommendation.repository;

import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ItemFeature;
import com.emall.recommendation.domain.UserBehaviorEvent;
import com.emall.recommendation.domain.UserPreference;
import java.util.List;
import java.util.Optional;

public interface RecommendationRepository {
    UserPreference saveUserPreference(UserPreference preference);

    Optional<UserPreference> findUserPreference(long userId, String categoryCode);

    List<UserPreference> findUserPreferences(long userId);

    ItemFeature saveItemFeature(ItemFeature feature);

    Optional<ItemFeature> findItemFeature(long skuId);

    List<ItemFeature> findActiveItemFeatures(int limit);

    UserBehaviorEvent saveBehaviorEvent(UserBehaviorEvent event);

    Experiment saveExperiment(Experiment experiment);

    Optional<Experiment> findExperiment(long experimentId);

    Optional<Experiment> findActiveExperiment(String scene);
}
