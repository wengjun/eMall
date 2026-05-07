package com.emall.intelligence;

import java.math.BigDecimal;
import java.time.Instant;

enum FeatureScope {
    USER,
    ITEM,
    MERCHANT,
    CONTEXT
}

enum ModelStatus {
    REGISTERED,
    APPROVED,
    DEPLOYED,
    ROLLED_BACK
}

record UserProfile(long profileId, long userId, String segment, String preferences, boolean privacyRestricted,
        Instant updatedAt) {
}

record ItemProfile(long profileId, long skuId, String category, String attributes, BigDecimal qualityScore,
        Instant updatedAt) {
}

record FeatureDefinition(long featureId, String featureName, FeatureScope scope, String owner, int freshnessSeconds,
        Instant createdAt) {
}

record OnlineFeatureValue(long valueId, String featureName, String entityKey, String featureValue, Instant eventTime,
        Instant updatedAt) {
}

record ModelDeployment(long modelId, String modelName, String version, String useCase, ModelStatus status,
        String approvalTicket, Instant createdAt, Instant updatedAt) {
    ModelDeployment changeStatus(ModelStatus nextStatus, String ticket) {
        return new ModelDeployment(modelId, modelName, version, useCase, nextStatus, ticket, createdAt, Instant.now());
    }
}

record AiDecision(long decisionId, String useCase, String entityKey, String decision, BigDecimal score,
        String modelVersion, Instant createdAt) {
}

record IntelligenceSummary(int userProfiles, int itemProfiles, int features, int deployedModels, int decisions) {
}
