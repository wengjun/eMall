package com.emall.experiment;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments")
class ExperimentController {
    private final ExperimentService experimentService;

    ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping
    ApiResponse<ExperimentDefinition> createExperiment(@Valid @RequestBody CreateExperimentRequest request) {
        return ApiResponse
                .ok(experimentService.createExperiment(request.scene(), request.name(), request.mutualExclusionGroup(),
                        request.trafficPercent(), request.controlVariant(), request.treatmentVariant()));
    }

    @PatchMapping("/{experimentId}/status")
    ApiResponse<ExperimentDefinition> changeStatus(@PathVariable long experimentId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(experimentService.changeStatus(experimentId, request.status()));
    }

    @PostMapping("/{experimentId}/guardrails")
    ApiResponse<GuardrailMetric> addGuardrail(@PathVariable long experimentId,
            @Valid @RequestBody AddGuardrailRequest request) {
        return ApiResponse.ok(experimentService.addGuardrail(experimentId, request.metricName(), request.direction(),
                request.threshold()));
    }

    @GetMapping("/assign")
    ApiResponse<ExperimentAssignment> assign(@RequestParam String scene, @RequestParam String userKey) {
        return ApiResponse.ok(experimentService.assign(scene, userKey));
    }

    @PostMapping("/{experimentId}/metrics")
    ApiResponse<ExperimentMetric> recordMetric(@PathVariable long experimentId,
            @Valid @RequestBody RecordMetricRequest request) {
        return ApiResponse.ok(
                experimentService.recordMetric(experimentId, request.variant(), request.metricName(), request.value()));
    }

    @GetMapping("/{experimentId}/report")
    ApiResponse<ExperimentReport> report(@PathVariable long experimentId, @RequestParam String metricName) {
        return ApiResponse.ok(experimentService.report(experimentId, metricName));
    }

    record CreateExperimentRequest(@NotBlank String scene, @NotBlank String name, @NotBlank String mutualExclusionGroup,
            @Min(0) @Max(100) int trafficPercent, @NotBlank String controlVariant, @NotBlank String treatmentVariant) {
    }

    record ChangeStatusRequest(@NotNull ExperimentStatus status) {
    }

    record AddGuardrailRequest(@NotBlank String metricName, @NotNull GuardrailDirection direction,
            @NotNull @DecimalMin("0.000000") BigDecimal threshold) {
    }

    record RecordMetricRequest(@NotBlank String variant, @NotBlank String metricName,
            @NotNull @DecimalMin("0.000000") BigDecimal value) {
    }
}
