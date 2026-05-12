package com.emall.pricing.rpc;

import com.emall.common.rpc.PriceQuoteCommand;
import com.emall.common.rpc.PriceQuoteView;
import com.emall.common.rpc.PricingRpcService;
import com.emall.pricing.domain.PriceQuote;
import com.emall.pricing.service.PricingService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DubboService
@ConditionalOnProperty(name = "emall.rpc.protocol", havingValue = "dubbo")
public class PricingDubboService implements PricingRpcService {
    private final PricingService pricingService;

    public PricingDubboService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Override
    public PriceQuoteView quote(PriceQuoteCommand command) {
        PriceQuote quote = pricingService.quote(command.skuId(), command.quantity());
        return new PriceQuoteView(quote.skuId(), quote.unitPrice(), quote.quantity(), quote.subtotal(),
                quote.currency(), quote.priceVersion(), quote.quotedAt());
    }
}
