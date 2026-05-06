package com.emall.payment.api;

import com.emall.common.api.ApiResponse;
import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ApiResponse<PaymentOrder> create(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.create(request.requestId(), request.orderId(), request.userId(),
                request.amount(), request.channel()));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentOrder> get(@PathVariable long paymentId) {
        return ApiResponse.ok(paymentService.get(paymentId));
    }

    @PostMapping("/{paymentId}/callbacks")
    public ApiResponse<PaymentOrder> callback(@PathVariable long paymentId,
                                              @Valid @RequestBody PaymentCallbackRequest request) {
        return ApiResponse.ok(paymentService.callback(request.channelTradeNo(), paymentId, request.paidAmount()));
    }

    @PostMapping("/{paymentId}/refund")
    public ApiResponse<PaymentOrder> refund(@PathVariable long paymentId) {
        return ApiResponse.ok(paymentService.refund(paymentId));
    }

    public record CreatePaymentRequest(
            @NotBlank String requestId,
            @Positive long orderId,
            @Positive long userId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String channel
    ) {
    }

    public record PaymentCallbackRequest(
            @NotBlank String channelTradeNo,
            @NotNull @DecimalMin("0.01") BigDecimal paidAmount
    ) {
    }
}
