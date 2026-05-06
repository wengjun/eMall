package com.emall.flashsale.api;

import com.emall.common.api.ApiResponse;
import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import com.emall.flashsale.service.FlashSaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/flash-sales")
public class FlashSaleController {
    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FlashSaleCampaign> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        return ApiResponse.ok(flashSaleService.createCampaign(request.skuId(), request.name(), request.startsAt(),
                request.endsAt(), request.perUserLimit(), request.tokenTtlSeconds(), request.queueCapacity()));
    }

    @GetMapping("/campaigns/{campaignId}")
    public ApiResponse<FlashSaleCampaign> getCampaign(@PathVariable long campaignId) {
        return ApiResponse.ok(flashSaleService.getCampaign(campaignId));
    }

    @PatchMapping("/campaigns/{campaignId}/status")
    public ApiResponse<FlashSaleCampaign> changeCampaignStatus(@PathVariable long campaignId,
                                                               @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(flashSaleService.changeCampaignStatus(campaignId, request.status()));
    }

    @PostMapping("/campaigns/{campaignId}/stock")
    public ApiResponse<FlashSaleStock> preallocateStock(@PathVariable long campaignId,
                                                        @Valid @RequestBody PreallocateStockRequest request) {
        return ApiResponse.ok(flashSaleService.preallocateStock(campaignId, request.totalStock()));
    }

    @GetMapping("/campaigns/{campaignId}/stock")
    public ApiResponse<FlashSaleStock> getStock(@PathVariable long campaignId) {
        return ApiResponse.ok(flashSaleService.getStock(campaignId));
    }

    @PostMapping("/campaigns/{campaignId}/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FlashSaleToken> issueToken(@PathVariable long campaignId,
                                                  @Valid @RequestBody IssueTokenRequest request) {
        return ApiResponse.ok(flashSaleService.issueToken(campaignId, request.userId(), request.quantity()));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<FlashSaleOrderRequest> enqueueOrder(@Valid @RequestBody EnqueueOrderRequest request) {
        return ApiResponse.ok(flashSaleService.enqueueOrder(request.token()));
    }

    @GetMapping("/orders/{requestId}")
    public ApiResponse<FlashSaleOrderRequest> getOrderRequest(@PathVariable long requestId) {
        return ApiResponse.ok(flashSaleService.getOrderRequest(requestId));
    }

    @GetMapping("/campaigns/{campaignId}/queue")
    public ApiResponse<List<FlashSaleOrderRequest>> findQueuedRequests(@PathVariable long campaignId,
                                                                       @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(flashSaleService.findQueuedRequests(campaignId, limit));
    }

    public record CreateCampaignRequest(
            @Positive long skuId,
            @NotBlank String name,
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startsAt,
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endsAt,
            @Positive int perUserLimit,
            @Positive int tokenTtlSeconds,
            @Positive int queueCapacity
    ) {
    }

    public record ChangeStatusRequest(@NotNull CampaignStatus status) {
    }

    public record PreallocateStockRequest(@Positive int totalStock) {
    }

    public record IssueTokenRequest(@Positive long userId, @Positive int quantity) {
    }

    public record EnqueueOrderRequest(@NotBlank String token) {
    }
}
