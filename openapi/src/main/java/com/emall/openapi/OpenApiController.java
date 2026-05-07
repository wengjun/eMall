package com.emall.openapi;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openapi")
class OpenApiController {
    private final OpenApiService openApiService;

    OpenApiController(OpenApiService openApiService) {
        this.openApiService = openApiService;
    }

    @PostMapping("/apps")
    ApiResponse<AppRegistration> registerApp(@Valid @RequestBody RegisterAppRequest request) {
        return ApiResponse.ok(openApiService.registerApp(request.merchantId(), request.name(), request.scopes(),
                request.dailyQuota()));
    }

    @PostMapping("/signatures/verify")
    ApiResponse<ApiSignatureVerification> verifySignature(@Valid @RequestBody VerifySignatureRequest request) {
        return ApiResponse.ok(openApiService.verifySignature(request.appKey(), request.appSecret(),
                request.requestPath(), request.nonce(), request.timestamp(), request.signature()));
    }

    @PostMapping("/apps/{appKey}/quota")
    ApiResponse<ApiQuotaUsage> consumeQuota(@PathVariable String appKey) {
        return ApiResponse.ok(openApiService.consumeQuota(appKey));
    }

    @PostMapping("/apps/{appId}/webhooks")
    ApiResponse<WebhookSubscription> subscribe(@PathVariable long appId,
            @Valid @RequestBody SubscribeWebhookRequest request) {
        return ApiResponse.ok(openApiService.subscribe(appId, request.eventType(), request.targetUrl()));
    }

    @GetMapping("/apps/{appId}/webhooks")
    ApiResponse<List<WebhookSubscription>> findSubscriptions(@PathVariable long appId) {
        return ApiResponse.ok(openApiService.findSubscriptions(appId));
    }

    @PostMapping("/webhooks/{subscriptionId}/deliveries")
    ApiResponse<WebhookDelivery> recordDelivery(@PathVariable long subscriptionId,
            @Valid @RequestBody RecordDeliveryRequest request) {
        return ApiResponse.ok(openApiService.recordDelivery(subscriptionId, request.eventId(), request.status()));
    }

    record RegisterAppRequest(@Positive long merchantId, @NotBlank String name, @NotBlank String scopes,
            @Min(1) int dailyQuota) {
    }

    record VerifySignatureRequest(@NotBlank String appKey, @NotBlank String appSecret, @NotBlank String requestPath,
            @NotBlank String nonce, @Min(1) long timestamp, @NotBlank String signature) {
    }

    record SubscribeWebhookRequest(@NotBlank String eventType, @NotBlank String targetUrl) {
    }

    record RecordDeliveryRequest(@NotBlank String eventId, @NotNull WebhookDeliveryStatus status) {
    }
}
