package com.emall.traffic;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traffic")
class TrafficController {
    private final TrafficService trafficService;

    TrafficController(TrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @PostMapping("/units")
    ApiResponse<UnitCell> registerUnit(@Valid @RequestBody RegisterUnitRequest request) {
        return ApiResponse
                .ok(trafficService.registerUnit(request.unitCode(), request.regionCode(), request.capacityWeight()));
    }

    @PostMapping("/shard-routes")
    ApiResponse<ShardRoute> routeShard(@Valid @RequestBody RouteShardRequest request) {
        return ApiResponse.ok(trafficService.routeShard(request.domainName(), request.shardNo(), request.unitCode(),
                request.databaseKey()));
    }

    @PostMapping("/shifts")
    ApiResponse<TrafficShift> planShift(@Valid @RequestBody PlanShiftRequest request) {
        return ApiResponse.ok(trafficService.planShift(request.sourceUnit(), request.targetUnit(), request.percent(),
                request.reason()));
    }

    @PatchMapping("/shifts/{shiftId}/status")
    ApiResponse<TrafficShift> changeShiftStatus(@PathVariable long shiftId,
            @Valid @RequestBody ChangeShiftStatusRequest request) {
        return ApiResponse.ok(trafficService.changeShiftStatus(shiftId, request.status()));
    }

    @PatchMapping("/units/{unitCode}/isolate")
    ApiResponse<UnitCell> isolateUnit(@PathVariable String unitCode) {
        return ApiResponse.ok(trafficService.isolateUnit(unitCode));
    }

    @GetMapping("/summary")
    ApiResponse<TrafficSummary> summary() {
        return ApiResponse.ok(trafficService.summary());
    }

    record RegisterUnitRequest(@NotBlank String unitCode, @NotBlank String regionCode, @Positive int capacityWeight) {
    }

    record RouteShardRequest(@NotBlank String domainName, @Min(0) int shardNo, @NotBlank String unitCode,
            @NotBlank String databaseKey) {
    }

    record PlanShiftRequest(@NotBlank String sourceUnit, @NotBlank String targetUnit, @Min(0) @Max(100) int percent,
            @NotBlank String reason) {
    }

    record ChangeShiftStatusRequest(ShiftStatus status) {
    }
}
