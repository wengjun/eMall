package com.emall.common.rpc;

public interface MarketingRpcService {
    PromotionQuoteView quote(PromotionQuoteCommand command);
}
