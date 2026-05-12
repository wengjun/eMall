package com.emall.payment.integration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.emall.common.rpc.OrderPaymentCommand;
import com.emall.common.rpc.OrderRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrderClient {
    private final RestClient orderRestClient;
    private final String rpcProtocol;

    @DubboReference(check = false, retries = 0, timeout = 500)
    private OrderRpcService orderRpcService;

    public OrderClient(RestClient orderRestClient, @Value("${emall.rpc.protocol:http}") String rpcProtocol) {
        this.orderRestClient = orderRestClient;
        this.rpcProtocol = rpcProtocol;
    }

    public OrderClient(RestClient orderRestClient) {
        this(orderRestClient, "http");
    }

    @SentinelResource(value = "payment.order.pay", blockHandler = "blockPay", fallback = "fallbackPay")
    public boolean payOrder(long orderId) {
        if (dubboEnabled()) {
            return orderRpcService.payOrder(new OrderPaymentCommand(orderId));
        }
        orderRestClient.post().uri("/api/orders/{orderId}/pay", orderId).retrieve().toBodilessEntity();
        return true;
    }

    public boolean fallbackPay(long orderId, Throwable error) {
        return false;
    }

    public boolean blockPay(long orderId, BlockException error) {
        return false;
    }

    private boolean dubboEnabled() {
        return "dubbo".equalsIgnoreCase(rpcProtocol) && orderRpcService != null;
    }
}
