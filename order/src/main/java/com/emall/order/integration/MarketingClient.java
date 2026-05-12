package com.emall.order.integration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.emall.common.rpc.MarketingRpcService;
import com.emall.common.rpc.PromotionQuoteCommand;
import com.emall.common.rpc.PromotionQuoteView;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MarketingClient {
    private final RestClient marketingRestClient;
    private final String rpcProtocol;

    @DubboReference(check = false, retries = 0, timeout = 300)
    private MarketingRpcService marketingRpcService;

    public MarketingClient(RestClient marketingRestClient, @Value("${emall.rpc.protocol:http}") String rpcProtocol) {
        this.marketingRestClient = marketingRestClient;
        this.rpcProtocol = rpcProtocol;
    }

    public MarketingClient(RestClient marketingRestClient) {
        this(marketingRestClient, "http");
    }

    @SentinelResource(value = "order.marketing.quote", blockHandler = "blockQuote", fallback = "fallbackQuote")
    public PromotionQuote quote(long userId, BigDecimal orderAmount) {
        PromotionQuote result;
        if (dubboEnabled()) {
            result = toLocal(marketingRpcService.quote(new PromotionQuoteCommand(userId, orderAmount)));
        } else {
            PromotionQuoteResponse response = marketingRestClient.post().uri("/api/marketing/quotes")
                    .body(new PromotionQuoteRequest(userId, orderAmount)).retrieve()
                    .body(PromotionQuoteResponse.class);
            result = response == null ? null : response.data();
        }
        return result == null ? PromotionQuote.none(userId, orderAmount) : result;
    }

    public PromotionQuote fallbackQuote(long userId, BigDecimal orderAmount, Throwable error) {
        return PromotionQuote.none(userId, orderAmount);
    }

    public PromotionQuote blockQuote(long userId, BigDecimal orderAmount, BlockException error) {
        return PromotionQuote.none(userId, orderAmount);
    }

    private boolean dubboEnabled() {
        return "dubbo".equalsIgnoreCase(rpcProtocol) && marketingRpcService != null;
    }

    private PromotionQuote toLocal(PromotionQuoteView view) {
        return view == null ? null : new PromotionQuote(view.userId(), view.orderAmount(), view.discountAmount(),
                view.payableAmount(), view.couponId(), view.quotedAt());
    }

    public record PromotionQuoteRequest(long userId, BigDecimal orderAmount) {
    }

    public record PromotionQuote(long userId, BigDecimal orderAmount, BigDecimal discountAmount,
            BigDecimal payableAmount, String couponId, Instant quotedAt) {
        public static PromotionQuote none(long userId, BigDecimal orderAmount) {
            return new PromotionQuote(userId, orderAmount, BigDecimal.ZERO, orderAmount, null, Instant.now());
        }
    }

    public record PromotionQuoteResponse(boolean success, String code, String message, PromotionQuote data,
            Instant timestamp) {
    }
}
