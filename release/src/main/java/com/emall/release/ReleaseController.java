package com.emall.release;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/release")
class ReleaseController {
    private final ReleaseService releaseService;

    ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @PostMapping("/feature-toggles")
    ApiResponse<FeatureToggle> createToggle(@Valid @RequestBody CreateToggleRequest request) {
        return ApiResponse.ok(releaseService.createToggle(request.flagKey(), request.serviceName(), request.status(),
                request.rolloutPercent()));
    }

    @PatchMapping("/feature-toggles/{toggleId}")
    ApiResponse<FeatureToggle> updateToggle(@PathVariable long toggleId,
            @Valid @RequestBody UpdateToggleRequest request) {
        return ApiResponse.ok(releaseService.updateToggle(toggleId, request.status(), request.rolloutPercent()));
    }

    @PostMapping("/rollouts")
    ApiResponse<RolloutPlan> createRollout(@Valid @RequestBody CreateRolloutRequest request) {
        return ApiResponse.ok(releaseService.createRollout(request.serviceName(), request.version(), request.strategy(),
                request.currentPercent()));
    }

    @PatchMapping("/rollouts/{rolloutId}/status")
    ApiResponse<RolloutPlan> changeRollout(@PathVariable long rolloutId,
            @Valid @RequestBody ChangeRolloutRequest request) {
        return ApiResponse.ok(releaseService.changeRollout(rolloutId, request.status(), request.currentPercent()));
    }

    @PostMapping("/topics")
    ApiResponse<MessageTopicGovernance> registerTopic(@Valid @RequestBody RegisterTopicRequest request) {
        return ApiResponse.ok(releaseService.registerTopic(request.topicName(), request.owner(),
                request.schemaVersion(), request.lagBudget()));
    }

    @PatchMapping("/topics/{topicId}/status")
    ApiResponse<MessageTopicGovernance> changeTopicStatus(@PathVariable long topicId,
            @Valid @RequestBody ChangeTopicRequest request) {
        return ApiResponse.ok(releaseService.changeTopicStatus(topicId, request.status()));
    }

    @PostMapping("/replay-plans")
    ApiResponse<ReplayPlan> createReplay(@Valid @RequestBody CreateReplayRequest request) {
        return ApiResponse.ok(releaseService.createReplay(request.topicName(), request.consumerGroup(),
                request.fromOffset(), request.toOffset()));
    }

    @PatchMapping("/replay-plans/{replayId}/status")
    ApiResponse<ReplayPlan> changeReplayStatus(@PathVariable long replayId,
            @Valid @RequestBody ChangeReplayRequest request) {
        return ApiResponse.ok(releaseService.changeReplayStatus(replayId, request.status()));
    }

    @GetMapping("/summary")
    ApiResponse<ReleaseSummary> summary() {
        return ApiResponse.ok(releaseService.summary());
    }

    record CreateToggleRequest(@NotBlank String flagKey, @NotBlank String serviceName, ToggleStatus status,
            @Min(0) @Max(100) int rolloutPercent) {
    }

    record UpdateToggleRequest(ToggleStatus status, @Min(0) @Max(100) int rolloutPercent) {
    }

    record CreateRolloutRequest(@NotBlank String serviceName, @NotBlank String version, @NotBlank String strategy,
            @Min(0) @Max(100) int currentPercent) {
    }

    record ChangeRolloutRequest(RolloutStatus status, @Min(0) @Max(100) int currentPercent) {
    }

    record RegisterTopicRequest(@NotBlank String topicName, @NotBlank String owner, @NotBlank String schemaVersion,
            @Positive long lagBudget) {
    }

    record ChangeTopicRequest(TopicStatus status) {
    }

    record CreateReplayRequest(@NotBlank String topicName, @NotBlank String consumerGroup,
            @PositiveOrZero long fromOffset, @PositiveOrZero long toOffset) {
    }

    record ChangeReplayRequest(RolloutStatus status) {
    }
}
