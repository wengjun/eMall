package com.emall.order.integration;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PricingClient {
    private final RestClient pricingRestClient;

    public PricingClient(RestClient pricingRestClient) {
        this.pricingRestClient = pricingRestClient;
    }

    @Retry(name = "pricingService")
    @RateLimiter(name = "pricingService")
    @Bulkhead(name = "pricingService")
    @CircuitBreaker(name = "pricingService", fallbackMethod = "fallbackQuote")
    public PriceQuote quote(long skuId, int quantity) {
        PriceQuoteResponse response = pricingRestClient.post().uri("/api/prices/quotes")
                .body(new QuoteRequest(skuId, quantity)).retrieve().body(PriceQuoteResponse.class);
        PriceQuote result = response == null ? null : response.data();
        if (result == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "price quote returned empty response");
        }
        return result;
    }

    public PriceQuote fallbackQuote(long skuId, int quantity, Throwable error) {
        throw new BusinessException(ErrorCode.CONFLICT, "pricing service unavailable");
    }

    public record QuoteRequest(long skuId, int quantity) {
    }

    public record PriceQuote(long skuId, BigDecimal unitPrice, int quantity, BigDecimal subtotal, String currency,
            long priceVersion, Instant quotedAt) {
    }

    public record PriceQuoteResponse(boolean success, String code, String message, PriceQuote data, Instant timestamp) {
    }
}
