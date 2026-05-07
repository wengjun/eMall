package com.emall.intelligence;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligence")
class IntelligenceController {
    private final IntelligenceService intelligenceService;

    IntelligenceController(IntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @PostMapping("/user-profiles")
    ApiResponse<UserProfile> upsertUserProfile(@Valid @RequestBody UpsertUserProfileRequest request) {
        return ApiResponse.ok(intelligenceService.upsertUserProfile(request.userId(), request.segment(),
                request.preferences(), request.privacyRestricted()));
    }

    @PostMapping("/item-profiles")
    ApiResponse<ItemProfile> upsertItemProfile(@Valid @RequestBody UpsertItemProfileRequest request) {
        return ApiResponse.ok(intelligenceService.upsertItemProfile(request.skuId(), request.category(),
                request.attributes(), request.qualityScore()));
    }

    @PostMapping("/features")
    ApiResponse<FeatureDefinition> registerFeature(@Valid @RequestBody RegisterFeatureRequest request) {
        return ApiResponse.ok(intelligenceService.registerFeature(request.featureName(), request.scope(),
                request.owner(), request.freshnessSeconds()));
    }

    @PostMapping("/feature-values")
    ApiResponse<OnlineFeatureValue> writeFeatureValue(@Valid @RequestBody WriteFeatureValueRequest request) {
        return ApiResponse.ok(intelligenceService.writeFeatureValue(request.featureName(), request.entityKey(),
                request.featureValue(), request.eventTime()));
    }

    @PostMapping("/models")
    ApiResponse<ModelDeployment> registerModel(@Valid @RequestBody RegisterModelRequest request) {
        return ApiResponse
                .ok(intelligenceService.registerModel(request.modelName(), request.version(), request.useCase()));
    }

    @PatchMapping("/models/{modelId}/status")
    ApiResponse<ModelDeployment> changeModelStatus(@PathVariable long modelId,
            @Valid @RequestBody ChangeModelStatusRequest request) {
        return ApiResponse
                .ok(intelligenceService.changeModelStatus(modelId, request.status(), request.approvalTicket()));
    }

    @PostMapping("/decisions")
    ApiResponse<AiDecision> recordDecision(@Valid @RequestBody RecordDecisionRequest request) {
        return ApiResponse.ok(intelligenceService.recordDecision(request.useCase(), request.entityKey(),
                request.decision(), request.score(), request.modelVersion()));
    }

    @GetMapping("/summary")
    ApiResponse<IntelligenceSummary> summary() {
        return ApiResponse.ok(intelligenceService.summary());
    }

    record UpsertUserProfileRequest(@Positive long userId, @NotBlank String segment, @NotBlank String preferences,
            boolean privacyRestricted) {
    }

    record UpsertItemProfileRequest(@Positive long skuId, @NotBlank String category, @NotBlank String attributes,
            @DecimalMin("0.00") BigDecimal qualityScore) {
    }

    record RegisterFeatureRequest(@NotBlank String featureName, FeatureScope scope, @NotBlank String owner,
            @Positive int freshnessSeconds) {
    }

    record WriteFeatureValueRequest(@NotBlank String featureName, @NotBlank String entityKey,
            @NotBlank String featureValue, Instant eventTime) {
    }

    record RegisterModelRequest(@NotBlank String modelName, @NotBlank String version, @NotBlank String useCase) {
    }

    record ChangeModelStatusRequest(ModelStatus status, @NotBlank String approvalTicket) {
    }

    record RecordDecisionRequest(@NotBlank String useCase, @NotBlank String entityKey, @NotBlank String decision,
            @DecimalMin("0.00") BigDecimal score, @NotBlank String modelVersion) {
    }
}
