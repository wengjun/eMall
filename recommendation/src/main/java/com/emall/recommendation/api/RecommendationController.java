package com.emall.recommendation.api;

import com.emall.common.api.ApiResponse;
import com.emall.recommendation.domain.BehaviorType;
import com.emall.recommendation.domain.Experiment;
import com.emall.recommendation.domain.ExperimentStatus;
import com.emall.recommendation.domain.ItemFeature;
import com.emall.recommendation.domain.RecommendationItem;
import com.emall.recommendation.domain.UserBehaviorEvent;
import com.emall.recommendation.domain.UserPreference;
import com.emall.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/users/{userId}/preferences")
    public ApiResponse<UserPreference> upsertPreference(@PathVariable long userId,
            @Valid @RequestBody UpsertPreferenceRequest request) {
        return ApiResponse
                .ok(recommendationService.upsertPreference(userId, request.categoryCode(), request.affinityScore()));
    }

    @GetMapping("/users/{userId}/preferences")
    public ApiResponse<List<UserPreference>> findPreferences(@PathVariable long userId) {
        return ApiResponse.ok(recommendationService.findPreferences(userId));
    }

    @PostMapping("/items")
    public ApiResponse<ItemFeature> upsertItemFeature(@Valid @RequestBody UpsertItemRequest request) {
        return ApiResponse.ok(recommendationService.upsertItemFeature(request.skuId(), request.categoryCode(),
                request.baseScore(), request.popularityScore(), request.active()));
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<UserBehaviorEvent> recordBehavior(@Valid @RequestBody RecordBehaviorRequest request) {
        return ApiResponse.ok(recommendationService.recordBehavior(request.userId(), request.skuId(),
                request.categoryCode(), request.behaviorType(), request.occurredAt()));
    }

    @PostMapping("/experiments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Experiment> createExperiment(@Valid @RequestBody CreateExperimentRequest request) {
        return ApiResponse.ok(recommendationService.createExperiment(request.scene(), request.name(),
                request.trafficPercent(), request.controlStrategy(), request.treatmentStrategy()));
    }

    @PatchMapping("/experiments/{experimentId}/status")
    public ApiResponse<Experiment> changeExperimentStatus(@PathVariable long experimentId,
            @Valid @RequestBody ChangeExperimentStatusRequest request) {
        return ApiResponse.ok(recommendationService.changeExperimentStatus(experimentId, request.status()));
    }

    @GetMapping
    public ApiResponse<List<RecommendationItem>> recommend(@RequestParam long userId,
            @RequestParam(defaultValue = "home") String scene, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(recommendationService.recommend(userId, scene, limit));
    }

    public record UpsertPreferenceRequest(@NotBlank String categoryCode, @Min(0) @Max(100) int affinityScore) {
    }

    public record UpsertItemRequest(@Positive long skuId, @NotBlank String categoryCode,
            @NotNull @DecimalMin("0.000000") BigDecimal baseScore,
            @NotNull @DecimalMin("0.000000") BigDecimal popularityScore, boolean active) {
    }

    public record RecordBehaviorRequest(@Positive long userId, @Positive long skuId, @NotBlank String categoryCode,
            @NotNull BehaviorType behaviorType,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAt) {
    }

    public record CreateExperimentRequest(@NotBlank String scene, @NotBlank String name,
            @Min(0) @Max(100) int trafficPercent, @NotBlank String controlStrategy,
            @NotBlank String treatmentStrategy) {
    }

    public record ChangeExperimentStatusRequest(@NotNull ExperimentStatus status) {
    }
}
