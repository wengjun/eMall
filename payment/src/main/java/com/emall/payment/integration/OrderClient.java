package com.emall.payment.integration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.emall.common.api.ApiResponse;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.rpc.OrderPaymentCommand;
import com.emall.common.rpc.OrderPaymentConfirmationCommand;
import com.emall.common.rpc.OrderPaymentSnapshot;
import com.emall.common.rpc.OrderRpcService;
import java.math.BigDecimal;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrderClient {
    private static final ParameterizedTypeReference<ApiResponse<OrderPaymentSnapshot>> SNAPSHOT_RESPONSE =
            new ParameterizedTypeReference<>() {
            };
    private final RestClient orderRestClient;
    private final String rpcProtocol;
    private final String serviceToken;

    @DubboReference(check = false, retries = 0, timeout = 2000)
    private OrderRpcService orderRpcService;

    @Autowired
    public OrderClient(RestClient orderRestClient, @Value("${emall.rpc.protocol:http}") String rpcProtocol,
            @Value("${emall.rpc.service-token:}") String serviceToken) {
        this.orderRestClient = orderRestClient;
        this.rpcProtocol = rpcProtocol;
        this.serviceToken = serviceToken;
    }

    public OrderClient(RestClient orderRestClient) {
        this(orderRestClient, "http", "");
    }

    @SentinelResource(value = "payment.order.snapshot", fallback = "fallbackSnapshot")
    public OrderPaymentSnapshot paymentSnapshot(long orderId) {
        if (dubboEnabled()) {
            return orderRpcService.paymentSnapshot(orderId);
        }
        RestClient.RequestHeadersSpec<?> request =
                orderRestClient.get().uri("/api/orders/{orderId}/payment-snapshot", orderId);
        ApiResponse<OrderPaymentSnapshot> response = authorize(request).retrieve().body(SNAPSHOT_RESPONSE);
        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "order payment snapshot is unavailable");
        }
        return response.data();
    }

    @SentinelResource(value = "payment.order.confirm", blockHandler = "blockConfirm", fallback = "fallbackConfirm")
    public boolean confirmPayment(long orderId, long paymentId, BigDecimal paidAmount, String currency,
            String channelTradeNo) {
        OrderPaymentConfirmationCommand command =
                new OrderPaymentConfirmationCommand(orderId, paymentId, paidAmount, currency, channelTradeNo);
        if (dubboEnabled()) {
            return orderRpcService.confirmPayment(command);
        }
        RestClient.RequestBodySpec request =
                orderRestClient.post().uri("/api/orders/{orderId}/confirm-payment", orderId);
        authorizeBody(request).body(command).retrieve().toBodilessEntity();
        return true;
    }

    @SentinelResource(value = "payment.order.pay", blockHandler = "blockPay", fallback = "fallbackPay")
    public boolean payOrder(long orderId) {
        if (dubboEnabled()) {
            return orderRpcService.payOrder(new OrderPaymentCommand(orderId));
        }
        RestClient.RequestBodySpec request = orderRestClient.post().uri("/api/orders/{orderId}/pay", orderId);
        authorize(request).retrieve().toBodilessEntity();
        return true;
    }

    public OrderPaymentSnapshot fallbackSnapshot(long orderId, Throwable error) {
        throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "order payment snapshot lookup failed");
    }

    public boolean fallbackConfirm(long orderId, long paymentId, BigDecimal paidAmount, String currency,
            String channelTradeNo, Throwable error) {
        return false;
    }

    public boolean blockConfirm(long orderId, long paymentId, BigDecimal paidAmount, String currency,
            String channelTradeNo, BlockException error) {
        return false;
    }

    public boolean fallbackPay(long orderId, Throwable error) {
        return false;
    }

    public boolean blockPay(long orderId, BlockException error) {
        return false;
    }

    private RestClient.RequestHeadersSpec<?> authorize(RestClient.RequestHeadersSpec<?> request) {
        return serviceToken == null || serviceToken.isBlank()
                ? request
                : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken);
    }

    private RestClient.RequestBodySpec authorizeBody(RestClient.RequestBodySpec request) {
        if (serviceToken != null && !serviceToken.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken);
        }
        return request;
    }

    private boolean dubboEnabled() {
        return "dubbo".equalsIgnoreCase(rpcProtocol) && orderRpcService != null;
    }
}
