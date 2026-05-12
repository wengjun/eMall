package com.emall.common.rpc;

public interface PricingRpcService {
    PriceQuoteView quote(PriceQuoteCommand command);
}
