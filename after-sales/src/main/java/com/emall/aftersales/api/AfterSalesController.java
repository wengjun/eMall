package com.emall.aftersales.api;

import com.emall.aftersales.domain.AfterSalesRequest;
import com.emall.aftersales.domain.AfterSalesType;
import com.emall.aftersales.service.AfterSalesService;
import com.emall.common.api.ApiResponse;
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
@RequestMapping("/api/after-sales")
public class AfterSalesController {
    private final AfterSalesService afterSalesService;

    public AfterSalesController(AfterSalesService afterSalesService) {
        this.afterSalesService = afterSalesService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AfterSalesRequest> create(@Valid @RequestBody CreateAfterSalesRequest request) {
        return ApiResponse.ok(afterSalesService.create(request.orderId(), request.userId(), request.skuId(),
                request.quantity(), request.refundAmount(), request.type(), request.reason()));
    }

    @GetMapping("/{requestId}")
    public ApiResponse<AfterSalesRequest> get(@PathVariable long requestId) {
        return ApiResponse.ok(afterSalesService.get(requestId));
    }

    @PostMapping("/{requestId}/approve")
    public ApiResponse<AfterSalesRequest> approve(@PathVariable long requestId) {
        return ApiResponse.ok(afterSalesService.approve(requestId));
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<AfterSalesRequest> reject(@PathVariable long requestId) {
        return ApiResponse.ok(afterSalesService.reject(requestId));
    }

    @PostMapping("/{requestId}/receive")
    public ApiResponse<AfterSalesRequest> receive(@PathVariable long requestId) {
        return ApiResponse.ok(afterSalesService.receive(requestId));
    }

    @PostMapping("/{requestId}/refund")
    public ApiResponse<AfterSalesRequest> refund(@PathVariable long requestId) {
        return ApiResponse.ok(afterSalesService.refund(requestId));
    }

    public record CreateAfterSalesRequest(@Positive long orderId, @Positive long userId, @Positive long skuId,
            @Positive int quantity, @NotNull @DecimalMin("0.01") BigDecimal refundAmount, @NotNull AfterSalesType type,
            @NotBlank String reason) {
    }
}
