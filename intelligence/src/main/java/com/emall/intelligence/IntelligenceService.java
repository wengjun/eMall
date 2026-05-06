package com.emall.intelligence;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IntelligenceService {
    private final IntelligenceRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    IntelligenceService(IntelligenceRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    UserProfile upsertUserProfile(long userId, String segment, String preferences, boolean privacyRestricted) {
        return repository.saveUserProfile(new UserProfile(idGenerator.nextId(), userId, normalize(segment),
                preferences, privacyRestricted, Instant.now()));
    }

    @Transactional
    ItemProfile upsertItemProfile(long skuId, String category, String attributes, BigDecimal qualityScore) {
        return repository.saveItemProfile(new ItemProfile(idGenerator.nextId(), skuId, normalize(category),
                attributes, qualityScore, Instant.now()));
    }

    @Transactional
    FeatureDefinition registerFeature(String featureName, FeatureScope scope, String owner, int freshnessSeconds) {
        if (freshnessSeconds <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "feature freshness must be positive");
        }
        return repository.saveFeature(new FeatureDefinition(idGenerator.nextId(), normalize(featureName), scope,
                normalize(owner), freshnessSeconds, Instant.now()));
    }

    @Transactional
    OnlineFeatureValue writeFeatureValue(String featureName, String entityKey, String featureValue,
                                         Instant eventTime) {
        return repository.saveFeatureValue(new OnlineFeatureValue(idGenerator.nextId(), normalize(featureName),
                normalize(entityKey), featureValue, eventTime, Instant.now()));
    }

    @Transactional
    ModelDeployment registerModel(String modelName, String version, String useCase) {
        Instant now = Instant.now();
        return repository.saveModel(new ModelDeployment(idGenerator.nextId(), normalize(modelName),
                normalize(version), normalize(useCase), ModelStatus.REGISTERED, "", now, now));
    }

    @Transactional
    ModelDeployment changeModelStatus(long modelId, ModelStatus status, String approvalTicket) {
        ModelDeployment model = repository.findModel(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "model deployment not found"));
        return repository.saveModel(model.changeStatus(status, approvalTicket));
    }

    @Transactional
    AiDecision recordDecision(String useCase, String entityKey, String decision, BigDecimal score,
                              String modelVersion) {
        return repository.saveDecision(new AiDecision(idGenerator.nextId(), normalize(useCase),
                normalize(entityKey), normalize(decision), score, normalize(modelVersion), Instant.now()));
    }

    IntelligenceSummary summary() {
        int deployedModels = (int) repository.findModels().stream()
                .filter(model -> model.status() == ModelStatus.DEPLOYED)
                .count();
        return new IntelligenceSummary(repository.findUserProfiles().size(), repository.findItemProfiles().size(),
                repository.findFeatures().size(), deployedModels, repository.findDecisions().size());
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "intelligence value must not be blank");
        }
        return normalized;
    }
}
