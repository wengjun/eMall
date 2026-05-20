package com.emall.order.workflow;

import com.emall.common.trust.ClientTrustContext;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
import com.emall.order.integration.InventoryClient.InventoryReservation;
import com.emall.order.integration.MarketingClient.CouponReservation;
import com.emall.order.integration.MarketingClient.PromotionQuote;
import com.emall.order.integration.PricingClient.PriceQuote;

public record OrderCreateContext(String requestId, long userId, long skuId, int quantity,
        OrderClientContext clientContext, ClientTrustContext trustContext, PriceQuote priceQuote,
        PromotionQuote promotionQuote, CouponReservation couponReservation, InventoryReservation inventoryReservation,
        Order order) {
}
