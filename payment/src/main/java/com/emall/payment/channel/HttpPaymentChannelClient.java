package com.emall.payment.channel;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.web.OutboundHttpClientFactory;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "emall.payment.channel.mode", havingValue = "http")
public class HttpPaymentChannelClient implements PaymentChannelClient {
    private final RestClient restClient;
    private final PaymentChannelProperties properties;

    public HttpPaymentChannelClient(OutboundHttpClientFactory clientFactory, PaymentChannelProperties properties) {
        this.restClient = clientFactory.restClient("payment-channel", properties.getBaseUrl());
        this.properties = properties;
    }

    @Override
    public ChannelPaymentResult createPayment(String requestId, long paymentId, long orderId, BigDecimal amount,
            String currency, String channel) {
        return post("/v1/payments", new CreatePaymentRequest(requestId, paymentId, orderId, amount, currency, channel),
                ChannelPaymentResult.class);
    }

    @Override
    public ChannelPaymentResult queryPayment(String channel, String channelTradeNo) {
        return get("/v1/payments/{channel}/{tradeNo}", ChannelPaymentResult.class, channel, channelTradeNo);
    }

    @Override
    public ChannelRefundResult requestRefund(String requestId, long refundId, String channel, String channelTradeNo,
            BigDecimal amount, String currency) {
        return post("/v1/refunds", new RefundRequest(requestId, refundId, channel, channelTradeNo, amount, currency),
                ChannelRefundResult.class);
    }

    @Override
    public ChannelRefundResult queryRefund(String channel, String channelRefundNo) {
        return get("/v1/refunds/{channel}/{refundNo}", ChannelRefundResult.class, channel, channelRefundNo);
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        try {
            T response = restClient.post().uri(path).header("X-Channel-Api-Key", properties.getApiKey()).body(request)
                    .retrieve().body(responseType);
            return requireResponse(response);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private <T> T get(String path, Class<T> responseType, Object... variables) {
        try {
            T response = restClient.get().uri(path, variables).header("X-Channel-Api-Key", properties.getApiKey())
                    .retrieve().body(responseType);
            return requireResponse(response);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private <T> T requireResponse(T response) {
        if (response == null) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "payment channel returned an empty response");
        }
        return response;
    }

    private BusinessException unavailable(RestClientException cause) {
        return new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                "payment channel request failed: " + cause.getClass().getSimpleName());
    }

    private record CreatePaymentRequest(String requestId, long paymentId, long orderId, BigDecimal amount,
            String currency, String channel) {
    }

    private record RefundRequest(String requestId, long refundId, String channel, String channelTradeNo,
            BigDecimal amount, String currency) {
    }
}
