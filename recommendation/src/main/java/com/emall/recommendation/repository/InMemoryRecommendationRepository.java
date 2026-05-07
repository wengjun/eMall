package com.emall.recommendation.repository;

import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ExperimentStatus;
import com.emall.recommendation.domain.ItemFeature;
import com.emall.recommendation.domain.UserBehaviorEvent;
import com.emall.recommendation.domain.UserPreference;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryRecommendationRepository implements RecommendationRepository {
    private final ConcurrentMap<String, UserPreference> preferences = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ItemFeature> itemFeatures = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, UserBehaviorEvent> behaviorEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Experiment> experiments = new ConcurrentHashMap<>();

    @Override
    public UserPreference saveUserPreference(UserPreference preference) {
        preferences.put(preferenceKey(preference.userId(), preference.categoryCode()), preference);
        return preference;
    }

    @Override
    public Optional<UserPreference> findUserPreference(long userId, String categoryCode) {
        return Optional.ofNullable(preferences.get(preferenceKey(userId, categoryCode)));
    }

    @Override
    public List<UserPreference> findUserPreferences(long userId) {
        return preferences.values().stream().filter(preference -> preference.userId() == userId)
                .sorted(Comparator.comparingInt(UserPreference::affinityScore).reversed()).toList();
    }

    @Override
    public ItemFeature saveItemFeature(ItemFeature feature) {
        itemFeatures.put(feature.skuId(), feature);
        return feature;
    }

    @Override
    public Optional<ItemFeature> findItemFeature(long skuId) {
        return Optional.ofNullable(itemFeatures.get(skuId));
    }

    @Override
    public List<ItemFeature> findActiveItemFeatures(int limit) {
        return itemFeatures
                .values().stream().filter(ItemFeature::active).sorted(Comparator.comparing(ItemFeature::popularityScore)
                        .reversed().thenComparing(Comparator.comparing(ItemFeature::baseScore).reversed()))
                .limit(limit).toList();
    }

    @Override
    public UserBehaviorEvent saveBehaviorEvent(UserBehaviorEvent event) {
        behaviorEvents.put(event.eventId(), event);
        return event;
    }

    @Override
    public Experiment saveExperiment(Experiment experiment) {
        experiments.put(experiment.experimentId(), experiment);
        return experiment;
    }

    @Override
    public Optional<Experiment> findExperiment(long experimentId) {
        return Optional.ofNullable(experiments.get(experimentId));
    }

    @Override
    public Optional<Experiment> findActiveExperiment(String scene) {
        return experiments.values().stream().filter(experiment -> experiment.scene().equals(scene))
                .filter(experiment -> experiment.status() == ExperimentStatus.ACTIVE)
                .max(Comparator.comparing(Experiment::updatedAt));
    }

    private String preferenceKey(long userId, String categoryCode) {
        return userId + ":" + categoryCode;
    }
}
