package com.emall.order.rpc;

import com.emall.common.rpc.OrderPaymentCommand;
import com.emall.common.rpc.OrderPaymentConfirmationCommand;
import com.emall.common.rpc.OrderPaymentSnapshot;
import com.emall.common.rpc.OrderRpcService;
import com.emall.order.service.OrderService;
import com.emall.order.payment.OrderPaymentConfirmationService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DubboService
@ConditionalOnProperty(name = "emall.rpc.protocol", havingValue = "dubbo")
public class OrderDubboService implements OrderRpcService {
    private final OrderService orderService;
    private final OrderPaymentConfirmationService paymentConfirmationService;

    public OrderDubboService(OrderService orderService, OrderPaymentConfirmationService paymentConfirmationService) {
        this.orderService = orderService;
        this.paymentConfirmationService = paymentConfirmationService;
    }

    @Override
    public boolean payOrder(OrderPaymentCommand command) {
        return false;
    }

    @Override
    public OrderPaymentSnapshot paymentSnapshot(long orderId) {
        var order = orderService.get(orderId);
        return new OrderPaymentSnapshot(order.orderId(), order.userId(), order.payableAmount(), order.currency(),
                order.status().name());
    }

    @Override
    public boolean confirmPayment(OrderPaymentConfirmationCommand command) {
        return paymentConfirmationService.confirm(command);
    }
}
