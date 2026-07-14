package com.emall.payment.channel;

import java.math.BigDecimal;

public interface PaymentChannelClient {
    ChannelPaymentResult createPayment(String requestId, long paymentId, long orderId, BigDecimal amount,
            String currency, String channel);

    ChannelPaymentResult queryPayment(String channel, String channelTradeNo);

    ChannelRefundResult requestRefund(String requestId, long refundId, String channel, String channelTradeNo,
            BigDecimal amount, String currency);

    ChannelRefundResult queryRefund(String channel, String channelRefundNo);
}
