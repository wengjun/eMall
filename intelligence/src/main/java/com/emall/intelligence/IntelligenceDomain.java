package com.emall.intelligence;

import java.math.BigDecimal;
import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

@TableName("user_profile")
record UserProfile(@TableId(value = "profile_id", type = IdType.INPUT) long profileId, long userId, String segment,
        String preferences, boolean privacyRestricted, Instant updatedAt) {
}

@TableName("item_profile")
record ItemProfile(@TableId(value = "profile_id", type = IdType.INPUT) long profileId, long skuId, String category,
        String attributes, BigDecimal qualityScore, Instant updatedAt) {
}

@TableName("feature_definition")
record FeatureDefinition(@TableId(value = "feature_id", type = IdType.INPUT) long featureId, String featureName,
        FeatureScope scope, String owner, int freshnessSeconds, Instant createdAt) {
}

@TableName("online_feature_value")
record OnlineFeatureValue(@TableId(value = "value_id", type = IdType.INPUT) long valueId, String featureName,
        String entityKey, String featureValue, Instant eventTime, Instant updatedAt) {
}

@TableName("model_deployment")
record ModelDeployment(@TableId(value = "model_id", type = IdType.INPUT) long modelId, String modelName,
        String version, String useCase, ModelStatus status, String approvalTicket, Instant createdAt,
        Instant updatedAt) {
    ModelDeployment changeStatus(ModelStatus nextStatus, String ticket) {
        return new ModelDeployment(modelId, modelName, version, useCase, nextStatus, ticket, createdAt, Instant.now());
    }
}

@TableName("ai_decision")
record AiDecision(@TableId(value = "decision_id", type = IdType.INPUT) long decisionId, String useCase,
        String entityKey, String decision, BigDecimal score, String modelVersion, Instant createdAt) {
}

record IntelligenceSummary(int userProfiles, int itemProfiles, int features, int deployedModels, int decisions) {
}
