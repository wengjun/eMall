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
    private final ConcurrentMap<Long, Long> orderIdByPaymentId = new ConcurrentHashMap<>();

    @Override
    public PaymentOrder save(PaymentOrder payment) {
        byId.put(payment.paymentId(), payment);
        idByRequest.put(payment.requestId(), payment.paymentId());
        orderIdByPaymentId.put(payment.paymentId(), payment.orderId());
        if (payment.channelTradeNo() != null && !payment.channelTradeNo().isBlank()) {
            idByTradeNo.put(tradeKey(payment.channel(), payment.channelTradeNo()), payment.paymentId());
        }
        return payment;
    }

    @Override
    public void saveRoute(long paymentId, String requestId, long orderId, long userId) {
        idByRequest.putIfAbsent(requestId, paymentId);
        orderIdByPaymentId.putIfAbsent(paymentId, orderId);
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
    public Optional<PaymentOrder> findByChannelAndTradeNo(String channel, String channelTradeNo) {
        Long paymentId = idByTradeNo.get(tradeKey(channel, channelTradeNo));
        return paymentId == null ? Optional.empty() : findById(paymentId);
    }

    @Override
    public Optional<Long> findRouteOrderIdByPaymentId(long paymentId) {
        return Optional.ofNullable(orderIdByPaymentId.get(paymentId));
    }

    @Override
    public Optional<Long> findRouteOrderIdByRequestId(String requestId) {
        Long paymentId = idByRequest.get(requestId);
        return paymentId == null ? Optional.empty() : findRouteOrderIdByPaymentId(paymentId);
    }

    @Override
    public boolean updateStatus(long paymentId, PaymentStatus expectedStatus, PaymentOrder payment) {
        AtomicFlag updated = new AtomicFlag();
        byId.computeIfPresent(paymentId, (id, existing) -> {
            if (existing.status() != expectedStatus) {
                return existing;
            }
            updated.mark();
            if (payment.channelTradeNo() != null && !payment.channelTradeNo().isBlank()) {
                idByTradeNo.put(tradeKey(payment.channel(), payment.channelTradeNo()), payment.paymentId());
            }
            return payment;
        });
        return updated.value();
    }

    @Override
    public boolean markOrderConfirmed(long paymentId, PaymentStatus expectedStatus, PaymentOrder payment) {
        AtomicFlag updated = new AtomicFlag();
        byId.computeIfPresent(paymentId, (id, existing) -> {
            if (existing.status() != expectedStatus || existing.orderConfirmed()) {
                return existing;
            }
            updated.mark();
            return payment;
        });
        return updated.value();
    }

    @Override
    public List<PaymentOrder> findUnconfirmedByStatus(PaymentStatus status, int limit) {
        return byId.values().stream().filter(payment -> payment.status() == status)
                .filter(payment -> !payment.orderConfirmed()).sorted(Comparator.comparing(PaymentOrder::updatedAt))
                .limit(limit).toList();
    }

    private static final class AtomicFlag {
        private boolean value;

        void mark() {
            value = true;
        }

        boolean value() {
            return value;
        }
    }

    private String tradeKey(String channel, String channelTradeNo) {
        return channel + ":" + channelTradeNo;
    }
}
