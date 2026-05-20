package com.emall.payment.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import com.emall.common.trust.ClientTrustContext;
import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentStatus;
import com.emall.payment.service.PaymentCallbackCommand;
import com.emall.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentOrderResponse> create(@Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Account-Id", required = false) Long accountId,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        ClientTrustContext trustContext = ClientTrustContext.fromBearerHeader(accountId, authorization, deviceId,
                firstPresent(forwardedFor, realIp), request.channel());
        return ApiResponse.ok(toResponse(paymentService.create(request.requestId(), request.orderId(), request.userId(),
                request.amount(), request.channel(), trustContext)));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentOrderResponse> get(@PathVariable long paymentId) {
        return ApiResponse.ok(toResponse(paymentService.get(paymentId)));
    }

    @PostMapping("/{paymentId}/callbacks")
    public ApiResponse<PaymentOrderResponse> callback(@PathVariable long paymentId,
            @Valid @RequestBody PaymentCallbackRequest request) {
        return ApiResponse.ok(toResponse(
                paymentService.callback(new PaymentCallbackCommand(request.channel(), request.channelTradeNo(),
                        paymentId, request.paidAmount(), request.timestamp(), request.nonce(), request.signature()))));
    }

    @PostMapping("/{paymentId}/refund")
    public ApiResponse<PaymentOrderResponse> refund(@PathVariable long paymentId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Account-Id", required = false) Long accountId,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        ClientTrustContext trustContext = ClientTrustContext.fromBearerHeader(accountId, authorization, deviceId,
                firstPresent(forwardedFor, realIp), null);
        return ApiResponse.ok(toResponse(paymentService.refund(paymentId, trustContext)));
    }

    public record CreatePaymentRequest(@NotBlank String requestId, @Positive long orderId, @Positive long userId,
            @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String channel) {
    }

    public record PaymentCallbackRequest(@NotBlank String channel, @NotBlank String channelTradeNo,
            @NotNull @DecimalMin("0.01") BigDecimal paidAmount, @NotNull Instant timestamp, @NotBlank String nonce,
            @NotBlank String signature) {
    }

    public record PaymentOrderResponse(long paymentId, String requestId, long orderId, long userId, BigDecimal amount,
            String channel, String channelTradeNo, PaymentStatus status, boolean orderConfirmed, Instant createdAt,
            Instant updatedAt) {
    }

    private PaymentOrderResponse toResponse(PaymentOrder payment) {
        return new PaymentOrderResponse(payment.paymentId(), payment.requestId(), payment.orderId(), payment.userId(),
                payment.amount(), payment.channel(),
                SensitiveDataMasker.mask(SensitiveDataType.PAYMENT_REFERENCE, payment.channelTradeNo()),
                payment.status(), payment.orderConfirmed(), payment.createdAt(), payment.updatedAt());
    }

    private String firstPresent(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
