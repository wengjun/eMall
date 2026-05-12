package com.emall.marketing.rpc;

import com.emall.common.rpc.MarketingRpcService;
import com.emall.common.rpc.PromotionQuoteCommand;
import com.emall.common.rpc.PromotionQuoteView;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.service.MarketingService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DubboService
@ConditionalOnProperty(name = "emall.rpc.protocol", havingValue = "dubbo")
public class MarketingDubboService implements MarketingRpcService {
    private final MarketingService marketingService;

    public MarketingDubboService(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @Override
    public PromotionQuoteView quote(PromotionQuoteCommand command) {
        PromotionQuote quote = marketingService.quote(command.userId(), command.orderAmount());
        return new PromotionQuoteView(quote.userId(), quote.orderAmount(), quote.discountAmount(),
                quote.payableAmount(), quote.couponId(), quote.quotedAt());
    }
}
