package com.emall.advertising;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advertising")
class AdvertisingController {
    private final AdvertisingService advertisingService;

    AdvertisingController(AdvertisingService advertisingService) {
        this.advertisingService = advertisingService;
    }

    @PostMapping("/campaigns")
    ApiResponse<AdCampaign> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        return ApiResponse.ok(advertisingService.createCampaign(request.merchantId(), request.name(),
                request.dailyBudget(), request.bidAmount(), request.startsAt(), request.endsAt()));
    }

    @PatchMapping("/campaigns/{campaignId}/status")
    ApiResponse<AdCampaign> changeStatus(@PathVariable long campaignId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(advertisingService.changeStatus(campaignId, request.status()));
    }

    @PostMapping("/campaigns/{campaignId}/creatives")
    ApiResponse<AdCreative> addCreative(@PathVariable long campaignId, @Valid @RequestBody AddCreativeRequest request) {
        return ApiResponse
                .ok(advertisingService.addCreative(campaignId, request.skuId(), request.title(), request.targetUrl()));
    }

    @PostMapping("/campaigns/{campaignId}/targets")
    ApiResponse<KeywordTarget> addTarget(@PathVariable long campaignId, @Valid @RequestBody AddTargetRequest request) {
        return ApiResponse.ok(advertisingService.addTarget(campaignId, request.keyword(), request.bidMultiplier()));
    }

    @GetMapping("/rank")
    ApiResponse<SponsoredResult> rank(@RequestParam String keyword, @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(advertisingService.rank(keyword, limit));
    }

    @PostMapping("/campaigns/{campaignId}/events")
    ApiResponse<AdEvent> recordEvent(@PathVariable long campaignId, @Valid @RequestBody RecordEventRequest request) {
        return ApiResponse.ok(advertisingService.recordEvent(campaignId, request.creativeId(), request.eventType()));
    }

    record CreateCampaignRequest(@Positive long merchantId, @NotBlank String name,
            @NotNull @DecimalMin("0.01") BigDecimal dailyBudget, @NotNull @DecimalMin("0.01") BigDecimal bidAmount,
            @NotNull Instant startsAt, @NotNull Instant endsAt) {
    }

    record ChangeStatusRequest(@NotNull AdStatus status) {
    }

    record AddCreativeRequest(@Positive long skuId, @NotBlank String title, @NotBlank String targetUrl) {
    }

    record AddTargetRequest(@NotBlank String keyword, @NotNull @DecimalMin("0.01") BigDecimal bidMultiplier) {
    }

    record RecordEventRequest(@Positive long creativeId, @NotBlank String eventType) {
    }
}
