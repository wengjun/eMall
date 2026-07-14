package com.emall.order.payment;

import java.util.Optional;

public interface OrderPaymentConfirmationRepository {
    OrderPaymentConfirmation saveIfAbsent(OrderPaymentConfirmation confirmation);

    Optional<OrderPaymentConfirmation> findByOrderId(long orderId);
}
