package com.emall.payment.channel;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "emall.payment.channel.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryPaymentChannelClient implements PaymentChannelClient {
    private final Map<String, ChannelPaymentResult> payments = new ConcurrentHashMap<>();
    private final Map<String, ChannelRefundResult> refunds = new ConcurrentHashMap<>();

    @Override
    public ChannelPaymentResult createPayment(String requestId, long paymentId, long orderId, BigDecimal amount,
            String currency, String channel) {
        String tradeNo = "test-pay-" + UUID.randomUUID();
        ChannelPaymentResult result = new ChannelPaymentResult(tradeNo, ChannelOperationStatus.PROCESSING, amount,
                currency, "accepted by in-memory test channel");
        payments.put(channel + ':' + tradeNo, result);
        return result;
    }

    @Override
    public ChannelPaymentResult queryPayment(String channel, String channelTradeNo) {
        ChannelPaymentResult created = payments.get(channel + ':' + channelTradeNo);
        if (created == null) {
            return new ChannelPaymentResult(channelTradeNo, ChannelOperationStatus.FAILED, BigDecimal.ZERO, "CNY",
                    "payment does not exist in test channel");
        }
        return new ChannelPaymentResult(created.channelTradeNo(), ChannelOperationStatus.SUCCEEDED, created.amount(),
                created.currency(), "confirmed by in-memory test channel");
    }

    @Override
    public ChannelRefundResult requestRefund(String requestId, long refundId, String channel, String channelTradeNo,
            BigDecimal amount, String currency) {
        String refundNo = "test-refund-" + UUID.randomUUID();
        ChannelRefundResult result = new ChannelRefundResult(refundNo, ChannelOperationStatus.PROCESSING,
                "accepted by in-memory test channel");
        refunds.put(channel + ':' + refundNo, result);
        return result;
    }

    @Override
    public ChannelRefundResult queryRefund(String channel, String channelRefundNo) {
        ChannelRefundResult created = refunds.get(channel + ':' + channelRefundNo);
        return created == null
                ? new ChannelRefundResult(channelRefundNo, ChannelOperationStatus.FAILED,
                        "refund does not exist in test channel")
                : new ChannelRefundResult(channelRefundNo, ChannelOperationStatus.SUCCEEDED,
                        "confirmed by in-memory test channel");
    }
}
