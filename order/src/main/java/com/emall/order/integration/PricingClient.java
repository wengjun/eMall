package com.emall.order.integration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.rpc.PriceQuoteCommand;
import com.emall.common.rpc.PriceQuoteView;
import com.emall.common.rpc.PricingRpcService;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PricingClient {
    private final RestClient pricingRestClient;
    private final String rpcProtocol;

    @DubboReference(check = false, retries = 0, timeout = 300)
    private PricingRpcService pricingRpcService;

    public PricingClient(RestClient pricingRestClient, @Value("${emall.rpc.protocol:http}") String rpcProtocol) {
        this.pricingRestClient = pricingRestClient;
        this.rpcProtocol = rpcProtocol;
    }

    public PricingClient(RestClient pricingRestClient) {
        this(pricingRestClient, "http");
    }

    @SentinelResource(value = "order.pricing.quote", blockHandler = "blockQuote", fallback = "fallbackQuote")
    public PriceQuote quote(long skuId, int quantity) {
        PriceQuote result;
        if (dubboEnabled()) {
            result = toLocal(pricingRpcService.quote(new PriceQuoteCommand(skuId, quantity)));
        } else {
            PriceQuoteResponse response = pricingRestClient.post().uri("/api/prices/quotes")
                    .body(new QuoteRequest(skuId, quantity)).retrieve().body(PriceQuoteResponse.class);
            result = response == null ? null : response.data();
        }
        if (result == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "price quote returned empty response");
        }
        return result;
    }

    public PriceQuote fallbackQuote(long skuId, int quantity, Throwable error) {
        throw new BusinessException(ErrorCode.CONFLICT, "pricing service unavailable");
    }

    public PriceQuote blockQuote(long skuId, int quantity, BlockException error) {
        throw new BusinessException(ErrorCode.CONFLICT, "pricing service throttled");
    }

    private boolean dubboEnabled() {
        return "dubbo".equalsIgnoreCase(rpcProtocol) && pricingRpcService != null;
    }

    private PriceQuote toLocal(PriceQuoteView view) {
        return view == null ? null : new PriceQuote(view.skuId(), view.unitPrice(), view.quantity(), view.subtotal(),
                view.currency(), view.priceVersion(), view.quotedAt());
    }

    public record QuoteRequest(long skuId, int quantity) {
    }

    public record PriceQuote(long skuId, BigDecimal unitPrice, int quantity, BigDecimal subtotal, String currency,
            long priceVersion, Instant quotedAt) {
    }

    public record PriceQuoteResponse(boolean success, String code, String message, PriceQuote data, Instant timestamp) {
    }
}
