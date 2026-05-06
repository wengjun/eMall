package com.emall.payment.repository;

import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryPaymentRepository implements PaymentRepository {
    private final ConcurrentMap<Long, PaymentOrder> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> idByRequest = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> idByTradeNo = new ConcurrentHashMap<>();

    @Override
    public PaymentOrder save(PaymentOrder payment) {
        byId.put(payment.paymentId(), payment);
        idByRequest.put(payment.requestId(), payment.paymentId());
        if (payment.channelTradeNo() != null && !payment.channelTradeNo().isBlank()) {
            idByTradeNo.put(payment.channelTradeNo(), payment.paymentId());
        }
        return payment;
    }

    @Override
    public Optional<PaymentOrder> findById(long paymentId) {
        return Optional.ofNullable(byId.get(paymentId));
    }

    @Override
    public Optional<PaymentOrder> findByRequestId(String requestId) {
        Long paymentId = idByRequest.get(requestId);
        return paymentId == null ? Optional.empty() : findById(paymentId);
    }

    @Override
    public Optional<PaymentOrder> findByChannelTradeNo(String channelTradeNo) {
        Long paymentId = idByTradeNo.get(channelTradeNo);
        return paymentId == null ? Optional.empty() : findById(paymentId);
    }

    @Override
    public List<PaymentOrder> findUnconfirmedByStatus(PaymentStatus status, int limit) {
        return byId.values().stream()
                .filter(payment -> payment.status() == status)
                .filter(payment -> !payment.orderConfirmed())
                .sorted(Comparator.comparing(PaymentOrder::updatedAt))
                .limit(limit)
                .toList();
    }
}
