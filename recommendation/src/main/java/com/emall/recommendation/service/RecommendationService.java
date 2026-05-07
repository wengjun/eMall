package com.emall.recommendation.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.recommendation.domain.BehaviorType;
import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ExperimentStatus;
import com.emall.recommendation.domain.ItemFeature;
import com.emall.recommendation.domain.RankingStrategy;
import com.emall.recommendation.domain.RecommendationItem;
import com.emall.recommendation.domain.UserBehaviorEvent;
import com.emall.recommendation.domain.UserPreference;
import com.emall.recommendation.repository.RecommendationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private static final BigDecimal AFFINITY_NORMALIZER = new BigDecimal("100.00");

    private final RecommendationRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    public RecommendationService(RecommendationRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public UserPreference upsertPreference(long userId, String categoryCode, int affinityScore) {
        return repository.saveUserPreference(new UserPreference(userId, categoryCode, affinityScore, Instant.now()));
    }

    public List<UserPreference> findPreferences(long userId) {
        return repository.findUserPreferences(userId);
    }

    @Transactional
    public ItemFeature upsertItemFeature(long skuId, String categoryCode, BigDecimal baseScore,
            BigDecimal popularityScore, boolean active) {
        return repository.saveItemFeature(new ItemFeature(skuId, categoryCode, normalizeScore(baseScore),
                normalizeScore(popularityScore), active, Instant.now()));
    }

    @Transactional
    public UserBehaviorEvent recordBehavior(long userId, long skuId, String categoryCode, BehaviorType behaviorType,
            Instant occurredAt) {
        int weight = behaviorWeight(behaviorType);
        UserBehaviorEvent event = repository.saveBehaviorEvent(new UserBehaviorEvent(idGenerator.nextId(), userId,
                skuId, categoryCode, behaviorType, weight, occurredAt));
        int currentScore =
                repository.findUserPreference(userId, categoryCode).map(UserPreference::affinityScore).orElse(0);
        repository.saveUserPreference(
                new UserPreference(userId, categoryCode, Math.min(100, currentScore + weight), Instant.now()));
        return event;
    }

    @Transactional
    public Experiment createExperiment(String scene, String name, int trafficPercent, String controlStrategy,
            String treatmentStrategy) {
        if (trafficPercent < 0 || trafficPercent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "experiment traffic percent must be between 0 and 100");
        }
        validateStrategy(controlStrategy);
        validateStrategy(treatmentStrategy);
        Instant now = Instant.now();
        return repository.saveExperiment(new Experiment(idGenerator.nextId(), scene, name, trafficPercent,
                controlStrategy, treatmentStrategy, ExperimentStatus.DRAFT, now, now));
    }

    public Experiment getExperiment(long experimentId) {
        return repository.findExperiment(experimentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "recommendation experiment not found"));
    }

    @Transactional
    public Experiment changeExperimentStatus(long experimentId, ExperimentStatus status) {
        return repository.saveExperiment(getExperiment(experimentId).changeStatus(status));
    }

    public List<RecommendationItem> recommend(long userId, String scene, int limit) {
        ExperimentAssignment assignment = assignExperiment(userId, scene);
        RankingStrategy strategy = strategy(assignment.strategyCode());
        return repository.findActiveItemFeatures(Math.max(100, limit * 5)).stream()
                .map(feature -> rank(feature, userId, strategy, assignment.bucket()))
                .sorted(Comparator.comparing(RecommendationItem::score).reversed()
                        .thenComparingLong(RecommendationItem::skuId))
                .limit(Math.max(1, Math.min(limit, 100))).toList();
    }

    private RecommendationItem rank(ItemFeature feature, long userId, RankingStrategy strategy, String bucket) {
        BigDecimal affinityScore = repository.findUserPreference(userId, feature.categoryCode())
                .map(UserPreference::affinityScore).map(BigDecimal::new).orElse(BigDecimal.ZERO)
                .divide(AFFINITY_NORMALIZER, 6, RoundingMode.HALF_UP);
        BigDecimal score = feature.baseScore().multiply(strategy.baseWeight())
                .add(feature.popularityScore().multiply(strategy.popularityWeight()))
                .add(affinityScore.multiply(strategy.affinityWeight())).setScale(6, RoundingMode.HALF_UP);
        return new RecommendationItem(feature.skuId(), feature.categoryCode(), score, strategy.strategyCode(), bucket);
    }

    private ExperimentAssignment assignExperiment(long userId, String scene) {
        return repository.findActiveExperiment(scene).map(experiment -> assignmentFor(userId, scene, experiment))
                .orElse(new ExperimentAssignment("control", RankingStrategy.balanced().strategyCode()));
    }

    private ExperimentAssignment assignmentFor(long userId, String scene, Experiment experiment) {
        int bucketValue = Math.floorMod((userId + ":" + scene).hashCode(), 100);
        if (bucketValue < experiment.trafficPercent()) {
            return new ExperimentAssignment("treatment", experiment.treatmentStrategy());
        }
        return new ExperimentAssignment("control", experiment.controlStrategy());
    }

    private RankingStrategy strategy(String strategyCode) {
        return switch (strategyCode) {
            case "balanced" -> RankingStrategy.balanced();
            case "popularity" -> RankingStrategy.popularity();
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "unknown recommendation strategy");
        };
    }

    private void validateStrategy(String strategyCode) {
        strategy(strategyCode);
    }

    private int behaviorWeight(BehaviorType behaviorType) {
        return switch (behaviorType) {
            case VIEW -> 1;
            case CART -> 5;
            case FAVORITE -> 8;
            case PURCHASE -> 15;
        };
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "recommendation score must not be negative");
        }
        return score.setScale(6, RoundingMode.HALF_UP);
    }

    private record ExperimentAssignment(String bucket, String strategyCode) {
    }
}
