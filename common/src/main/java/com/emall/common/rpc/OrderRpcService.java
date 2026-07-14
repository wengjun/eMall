package com.emall.common.rpc;

public interface OrderRpcService {
    boolean payOrder(OrderPaymentCommand command);

    OrderPaymentSnapshot paymentSnapshot(long orderId);

    boolean confirmPayment(OrderPaymentConfirmationCommand command);
}
