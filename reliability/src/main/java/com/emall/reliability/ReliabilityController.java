package com.emall.reliability;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/reliability")
class ReliabilityController {
    private final ReliabilityService reliabilityService;

    ReliabilityController(ReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    @PostMapping("/capacity-rehearsals")
    ApiResponse<CapacityRehearsal> createRehearsal(@Valid @RequestBody CreateRehearsalRequest request) {
        return ApiResponse.ok(reliabilityService.createRehearsal(request.serviceName(), request.targetQps(),
                request.peakConcurrency()));
    }

    @PatchMapping("/capacity-rehearsals/{rehearsalId}/status")
    ApiResponse<CapacityRehearsal> changeRehearsalStatus(@PathVariable long rehearsalId,
                                                         @Valid @RequestBody ChangeGateStatusRequest request) {
        return ApiResponse.ok(reliabilityService.changeRehearsalStatus(rehearsalId, request.status()));
    }

    @PostMapping("/slos")
    ApiResponse<SloObjective> defineSlo(@Valid @RequestBody DefineSloRequest request) {
        return ApiResponse.ok(reliabilityService.defineSlo(request.serviceName(), request.availabilityTarget(),
                request.latencyP95Ms(), request.errorBudgetPercent()));
    }

    @PostMapping("/chaos-schedules")
    ApiResponse<ChaosSchedule> scheduleChaos(@Valid @RequestBody ScheduleChaosRequest request) {
        return ApiResponse.ok(reliabilityService.scheduleChaos(request.serviceName(), request.drillType(),
                request.blastRadiusPercent(), request.scheduledAt()));
    }

    @PatchMapping("/chaos-schedules/{chaosId}/approve")
    ApiResponse<ChaosSchedule> approveChaos(@PathVariable long chaosId) {
        return ApiResponse.ok(reliabilityService.approveChaos(chaosId));
    }

    @PostMapping("/readiness-gates")
    ApiResponse<ReadinessGate> evaluateReadiness(@Valid @RequestBody EvaluateReadinessRequest request) {
        return ApiResponse.ok(reliabilityService.evaluateReadiness(request.serviceName(), request.runbookReady(),
                request.dashboardReady(), request.rollbackReady()));
    }

    @GetMapping("/summary")
    ApiResponse<ReliabilitySummary> summary() {
        return ApiResponse.ok(reliabilityService.summary());
    }

    record CreateRehearsalRequest(@NotBlank String serviceName, @Positive int targetQps,
                                  @Positive int peakConcurrency) {
    }

    record ChangeGateStatusRequest(GateStatus status) {
    }

    record DefineSloRequest(@NotBlank String serviceName, @DecimalMin("0.0") BigDecimal availabilityTarget,
                            @Positive int latencyP95Ms, @DecimalMin("0.0") BigDecimal errorBudgetPercent) {
    }

    record ScheduleChaosRequest(@NotBlank String serviceName, @NotBlank String drillType,
                                @Min(0) @Max(100) int blastRadiusPercent, Instant scheduledAt) {
    }

    record EvaluateReadinessRequest(@NotBlank String serviceName, boolean runbookReady, boolean dashboardReady,
                                    boolean rollbackReady) {
    }
}
