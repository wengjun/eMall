package com.emall.common.rpc;

public interface OrderRpcService {
    boolean payOrder(OrderPaymentCommand command);
}
