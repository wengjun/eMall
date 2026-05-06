package com.emall.pricing.api;

import com.emall.common.api.ApiResponse;
import com.emall.pricing.domain.PriceBook;
import com.emall.pricing.domain.PriceQuote;
import com.emall.pricing.service.PricingService;
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
@RequestMapping("/api/prices")
public class PricingController {
    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PriceBook> upsert(@Valid @RequestBody UpsertPriceRequest request) {
        return ApiResponse.ok(pricingService.upsert(
                request.skuId(), request.listPrice(), request.salePrice(), request.currency(), request.active()));
    }

    @GetMapping("/{skuId}")
    public ApiResponse<PriceBook> get(@PathVariable long skuId) {
        return ApiResponse.ok(pricingService.get(skuId));
    }

    @PostMapping("/quotes")
    public ApiResponse<PriceQuote> quote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.ok(pricingService.quote(request.skuId(), request.quantity()));
    }

    public record UpsertPriceRequest(
            @Positive long skuId,
            @NotNull @DecimalMin("0.01") BigDecimal listPrice,
            @NotNull @DecimalMin("0.01") BigDecimal salePrice,
            @NotBlank String currency,
            boolean active
    ) {
    }

    public record QuoteRequest(@Positive long skuId, @Positive int quantity) {
    }
}
