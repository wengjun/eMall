package com.emall.payment.repository;

import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    PaymentOrder save(PaymentOrder payment);

    void saveRoute(long paymentId, String requestId, long orderId, long userId);

    Optional<PaymentOrder> findById(long paymentId);

    Optional<PaymentOrder> findByRequestId(String requestId);

    Optional<PaymentOrder> findByChannelAndTradeNo(String channel, String channelTradeNo);

    Optional<Long> findRouteOrderIdByPaymentId(long paymentId);

    Optional<Long> findRouteOrderIdByRequestId(String requestId);

    boolean updateStatus(long paymentId, PaymentStatus expectedStatus, PaymentOrder payment);

    boolean markOrderConfirmed(long paymentId, PaymentStatus expectedStatus, PaymentOrder payment);

    List<PaymentOrder> findUnconfirmedByStatus(PaymentStatus status, int limit);
}
