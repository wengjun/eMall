package com.emall.payment.repository;

import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    PaymentOrder save(PaymentOrder payment);

    Optional<PaymentOrder> findById(long paymentId);

    Optional<PaymentOrder> findByRequestId(String requestId);

    Optional<PaymentOrder> findByChannelTradeNo(String channelTradeNo);

    List<PaymentOrder> findUnconfirmedByStatus(PaymentStatus status, int limit);
}
