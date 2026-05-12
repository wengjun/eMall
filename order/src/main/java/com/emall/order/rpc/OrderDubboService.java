package com.emall.order.rpc;

import com.emall.common.rpc.OrderPaymentCommand;
import com.emall.common.rpc.OrderRpcService;
import com.emall.order.service.OrderService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DubboService
@ConditionalOnProperty(name = "emall.rpc.protocol", havingValue = "dubbo")
public class OrderDubboService implements OrderRpcService {
    private final OrderService orderService;

    public OrderDubboService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public boolean payOrder(OrderPaymentCommand command) {
        orderService.pay(command.orderId());
        return true;
    }
}
