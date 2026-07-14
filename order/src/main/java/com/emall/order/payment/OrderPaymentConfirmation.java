package com.emall.order.payment;

import com.emall.common.rpc.OrderPaymentConfirmationCommand;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderPaymentConfirmation(long orderId, long paymentId, BigDecimal paidAmount, String currency,
        String channelTradeNo, Instant confirmedAt) {
    public boolean matches(OrderPaymentConfirmationCommand command) {
        return paymentId == command.paymentId() && paidAmount.compareTo(command.paidAmount()) == 0
                && currency.equals(command.currency()) && channelTradeNo.equals(command.channelTradeNo());
    }
}
