package com.emall.order.integration;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MarketingClient {
    private final RestClient marketingRestClient;

    public MarketingClient(RestClient marketingRestClient) {
        this.marketingRestClient = marketingRestClient;
    }

    @Retry(name = "marketingService")
    @RateLimiter(name = "marketingService")
    @Bulkhead(name = "marketingService")
    @CircuitBreaker(name = "marketingService", fallbackMethod = "fallbackQuote")
    public PromotionQuote quote(long userId, BigDecimal orderAmount) {
        PromotionQuoteResponse response = marketingRestClient.post()
                .uri("/api/marketing/quotes")
                .body(new PromotionQuoteRequest(userId, orderAmount))
                .retrieve()
                .body(PromotionQuoteResponse.class);
        PromotionQuote result = response == null ? null : response.data();
        return result == null ? PromotionQuote.none(userId, orderAmount) : result;
    }

    public PromotionQuote fallbackQuote(long userId, BigDecimal orderAmount, Throwable error) {
        return PromotionQuote.none(userId, orderAmount);
    }

    public record PromotionQuoteRequest(long userId, BigDecimal orderAmount) {
    }

    public record PromotionQuote(
            long userId,
            BigDecimal orderAmount,
            BigDecimal discountAmount,
            BigDecimal payableAmount,
            String couponId,
            Instant quotedAt
    ) {
        public static PromotionQuote none(long userId, BigDecimal orderAmount) {
            return new PromotionQuote(userId, orderAmount, BigDecimal.ZERO, orderAmount, null, Instant.now());
        }
    }

    public record PromotionQuoteResponse(
            boolean success,
            String code,
            String message,
            PromotionQuote data,
            Instant timestamp
    ) {
    }
}
