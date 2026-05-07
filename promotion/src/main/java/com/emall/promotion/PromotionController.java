package com.emall.promotion;

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
@RequestMapping("/api/promotions")
class PromotionController {
    private final PromotionService promotionService;

    PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping("/campaigns")
    ApiResponse<PromotionCampaign> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        return ApiResponse.ok(promotionService.createCampaign(request.name(), request.type(), request.thresholdAmount(),
                request.benefitValue(), request.budgetAmount(), request.priority(), request.stackable(),
                request.startsAt(), request.endsAt()));
    }

    @PatchMapping("/campaigns/{campaignId}/status")
    ApiResponse<PromotionCampaign> changeStatus(@PathVariable long campaignId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(promotionService.changeStatus(campaignId, request.status()));
    }

    @PostMapping("/quotes")
    ApiResponse<PromotionQuote> quote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.ok(promotionService.quote(request.userId(), request.orderAmount()));
    }

    @GetMapping("/calendar")
    ApiResponse<CampaignCalendar> calendar(@RequestParam String month) {
        return ApiResponse.ok(promotionService.calendar(month));
    }

    record CreateCampaignRequest(@NotBlank String name, @NotNull PromotionType type,
            @NotNull @DecimalMin("0.00") BigDecimal thresholdAmount,
            @NotNull @DecimalMin("0.00") BigDecimal benefitValue, @NotNull @DecimalMin("0.01") BigDecimal budgetAmount,
            @Min(1) int priority, boolean stackable, @NotNull Instant startsAt, @NotNull Instant endsAt) {
    }

    record ChangeStatusRequest(@NotNull CampaignStatus status) {
    }

    record QuoteRequest(@Positive long userId, @NotNull @DecimalMin("0.01") BigDecimal orderAmount) {
    }
}
