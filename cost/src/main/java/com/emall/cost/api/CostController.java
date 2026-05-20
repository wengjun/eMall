package com.emall.cost.api;

import com.emall.common.api.ApiResponse;
import com.emall.cost.domain.CapacitySummary;
import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import com.emall.cost.domain.CostSummary;
import com.emall.cost.domain.ServiceCapacityBaseline;
import com.emall.cost.service.CostGovernanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/cost")
public class CostController {
    private final CostGovernanceService costGovernanceService;

    public CostController(CostGovernanceService costGovernanceService) {
        this.costGovernanceService = costGovernanceService;
    }

    @PostMapping("/signals")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<CostSignal> recordSignal(@Valid @RequestBody RecordSignalRequest request) {
        return ApiResponse.ok(costGovernanceService.recordSignal(request.serviceName(), request.signalType(),
                request.metricValue(), request.thresholdValue(), request.observedAt()));
    }

    @GetMapping("/services/{serviceName}/signals")
    public ApiResponse<List<CostSignal>> findSignals(@PathVariable String serviceName,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(costGovernanceService.findSignals(serviceName, limit));
    }

    @PostMapping("/budgets")
    public ApiResponse<CostBudget> upsertBudget(@Valid @RequestBody UpsertBudgetRequest request) {
        return ApiResponse.ok(costGovernanceService.upsertBudget(request.serviceName(), request.monthlyBudget(),
                request.currentSpend(), request.currency(), request.alertThresholdPercent(), request.active()));
    }

    @GetMapping("/services/{serviceName}/actions")
    public ApiResponse<List<CostOptimizationAction>> findActions(@PathVariable String serviceName) {
        return ApiResponse.ok(costGovernanceService.findActiveActions(serviceName));
    }

    @PatchMapping("/actions/{actionId}/status")
    public ApiResponse<CostOptimizationAction> changeActionStatus(@PathVariable long actionId,
            @Valid @RequestBody ChangeActionStatusRequest request) {
        return ApiResponse.ok(costGovernanceService.changeActionStatus(actionId, request.status()));
    }

    @GetMapping("/services/{serviceName}/summary")
    public ApiResponse<CostSummary> summary(@PathVariable String serviceName) {
        return ApiResponse.ok(costGovernanceService.summary(serviceName));
    }

    @PostMapping("/capacity-baselines")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<ServiceCapacityBaseline> recordCapacityBaseline(
            @Valid @RequestBody RecordCapacityBaselineRequest request) {
        return ApiResponse.ok(costGovernanceService.recordCapacityBaseline(request.serviceName(), request.safeQps(),
                request.peakQps(), request.currentQps(), request.currentReplicas(), request.maxReplicas(),
                request.cpuUtilization(), request.memoryUtilization(), request.monthlyCost(), request.sloProtected(),
                request.observedAt()));
    }

    @GetMapping("/services/{serviceName}/capacity")
    public ApiResponse<CapacitySummary> capacitySummary(@PathVariable String serviceName) {
        return ApiResponse.ok(costGovernanceService.capacitySummary(serviceName));
    }

    public record RecordSignalRequest(@NotBlank String serviceName, @NotNull CostSignalType signalType,
            @NotNull @DecimalMin("0.000000") BigDecimal metricValue,
            @NotNull @DecimalMin("0.000000") BigDecimal thresholdValue,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant observedAt) {
    }

    public record UpsertBudgetRequest(@NotBlank String serviceName,
            @NotNull @DecimalMin("0.000000") BigDecimal monthlyBudget,
            @NotNull @DecimalMin("0.000000") BigDecimal currentSpend, @NotBlank String currency,
            @Min(1) @Max(100) int alertThresholdPercent, boolean active) {
    }

    public record ChangeActionStatusRequest(@NotNull CostActionStatus status) {
    }

    public record RecordCapacityBaselineRequest(@NotBlank String serviceName, @Min(1) int safeQps, @Min(1) int peakQps,
            @Min(0) int currentQps, @Min(1) int currentReplicas, @Min(1) int maxReplicas,
            @NotNull @DecimalMin("0.000000") BigDecimal cpuUtilization,
            @NotNull @DecimalMin("0.000000") BigDecimal memoryUtilization,
            @NotNull @DecimalMin("0.000000") BigDecimal monthlyCost, boolean sloProtected,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant observedAt) {
    }
}
