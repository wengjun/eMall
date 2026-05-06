package com.emall.risk;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
class RiskController {
    private final RiskService riskService;

    RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/rules")
    ApiResponse<RiskRule> createRule(@Valid @RequestBody CreateRuleRequest request) {
        return ApiResponse.ok(riskService.createRule(request.scene(), request.ruleCode(), request.fieldName(),
                request.operator(), request.threshold(), request.level()));
    }

    @PatchMapping("/rules/{ruleId}/status")
    ApiResponse<RiskRule> changeRuleStatus(@PathVariable long ruleId,
                                           @Valid @RequestBody ChangeRuleStatusRequest request) {
        return ApiResponse.ok(riskService.changeRuleStatus(ruleId, request.status()));
    }

    @PostMapping("/devices")
    ApiResponse<DeviceReputation> upsertDevice(@Valid @RequestBody UpsertDeviceRequest request) {
        return ApiResponse.ok(riskService.upsertDevice(request.deviceId(), request.reputationScore(),
                request.risky()));
    }

    @PostMapping("/evaluate")
    ApiResponse<RiskDecision> evaluate(@Valid @RequestBody EvaluateRiskRequest request) {
        return ApiResponse.ok(riskService.evaluate(request.scene(), request.subjectId(), request.deviceId(),
                request.ip(), request.amount(), request.velocity()));
    }

    @GetMapping("/events")
    ApiResponse<List<RiskEvent>> findEvents(@RequestParam String subjectId) {
        return ApiResponse.ok(riskService.findEvents(subjectId));
    }

    record CreateRuleRequest(
            @NotNull RiskScene scene,
            @NotBlank String ruleCode,
            @NotBlank String fieldName,
            @NotNull RuleOperator operator,
            @NotNull @DecimalMin("0.000000") BigDecimal threshold,
            @NotNull RiskLevel level
    ) {
    }

    record ChangeRuleStatusRequest(@NotNull RuleStatus status) {
    }

    record UpsertDeviceRequest(@NotBlank String deviceId, @Min(0) @Max(100) int reputationScore, boolean risky) {
    }

    record EvaluateRiskRequest(
            @NotNull RiskScene scene,
            @NotBlank String subjectId,
            @NotBlank String deviceId,
            @NotBlank String ip,
            @NotNull @DecimalMin("0.000000") BigDecimal amount,
            @Min(0) int velocity
    ) {
    }
}
