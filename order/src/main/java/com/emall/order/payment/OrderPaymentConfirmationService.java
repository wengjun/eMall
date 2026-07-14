package com.emall.order.payment;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.rpc.OrderPaymentConfirmationCommand;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
import com.emall.order.service.OrderService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentConfirmationService {
    private final OrderService orderService;
    private final OrderPaymentConfirmationRepository repository;
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;

    public OrderPaymentConfirmationService(OrderService orderService, OrderPaymentConfirmationRepository repository,
            ShardRoutingOperations shardRoutingOperations, ShardRouteIndex shardRouteIndex) {
        this.orderService = orderService;
        this.repository = repository;
        this.shardRoutingOperations = shardRoutingOperations;
        this.shardRouteIndex = shardRouteIndex;
    }

    @Transactional
    public boolean confirm(OrderPaymentConfirmationCommand command) {
        validate(command);
        long routeKey =
                shardRouteIndex.resolveRequired("order-id", Long.toString(command.orderId()), command.orderId());
        return shardRoutingOperations.execute("order_record", routeKey, () -> confirmInShard(command));
    }

    private boolean confirmInShard(OrderPaymentConfirmationCommand command) {
        Order order = orderService.get(command.orderId());
        if (order.payableAmount().compareTo(command.paidAmount()) != 0
                || !order.currency().equals(command.currency())) {
            return false;
        }
        if (order.status() != OrderStatus.CREATED && order.status() != OrderStatus.PENDING_RETRY
                && order.status() != OrderStatus.PAID) {
            return false;
        }
        OrderPaymentConfirmation supplied = new OrderPaymentConfirmation(command.orderId(), command.paymentId(),
                command.paidAmount(), command.currency(), command.channelTradeNo(), Instant.now());
        OrderPaymentConfirmation persisted = repository.saveIfAbsent(supplied);
        if (!persisted.matches(command)) {
            throw new BusinessException(ErrorCode.CONFLICT, "order is already bound to another payment");
        }
        if (order.status() == OrderStatus.PAID) {
            return true;
        }
        orderService.pay(command.orderId());
        return true;
    }

    private void validate(OrderPaymentConfirmationCommand command) {
        if (command == null || command.orderId() <= 0 || command.paymentId() <= 0 || command.paidAmount() == null
                || command.paidAmount().signum() <= 0 || command.currency() == null || command.currency().isBlank()
                || command.channelTradeNo() == null || command.channelTradeNo().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "payment confirmation is incomplete");
        }
    }
}
