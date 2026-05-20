package com.emall.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusPaymentRepository implements PaymentRepository {
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRouteMapper routeMapper;

    public MybatisPlusPaymentRepository(PaymentOrderMapper paymentOrderMapper, PaymentRouteMapper routeMapper) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.routeMapper = routeMapper;
    }

    @Override
    public PaymentOrder save(PaymentOrder payment) {
        PaymentOrderEntity entity = toEntity(payment);
        try {
            paymentOrderMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            paymentOrderMapper.update(null,
                    new UpdateWrapper<PaymentOrderEntity>().set("channel_trade_no", entity.getChannelTradeNo())
                            .set("status", entity.getStatus()).set("order_confirmed", entity.getOrderConfirmed())
                            .set("updated_at", entity.getUpdatedAt()).eq("payment_id", entity.getPaymentId()));
        }
        return payment;
    }

    @Override
    public void saveRoute(long paymentId, String requestId, long orderId, long userId) {
        PaymentRouteEntity route = new PaymentRouteEntity();
        route.setPaymentId(paymentId);
        route.setRequestId(requestId);
        route.setOrderId(orderId);
        route.setUserId(userId);
        route.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            routeMapper.insert(route);
        } catch (DuplicateKeyException ex) {
            // The route index is idempotent for retried create requests.
        }
    }

    @Override
    public Optional<PaymentOrder> findById(long paymentId) {
        return Optional.ofNullable(paymentOrderMapper.selectById(paymentId)).map(this::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByRequestId(String requestId) {
        return Optional
                .ofNullable(paymentOrderMapper
                        .selectOne(new QueryWrapper<PaymentOrderEntity>().eq("request_id", requestId)))
                .map(this::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByChannelAndTradeNo(String channel, String channelTradeNo) {
        return Optional.ofNullable(paymentOrderMapper.selectOne(
                new QueryWrapper<PaymentOrderEntity>().eq("channel", channel).eq("channel_trade_no", channelTradeNo)))
                .map(this::toDomain);
    }

    @Override
    public Optional<Long> findRouteOrderIdByPaymentId(long paymentId) {
        return Optional.ofNullable(routeMapper.selectById(paymentId)).map(PaymentRouteEntity::getOrderId);
    }

    @Override
    public Optional<Long> findRouteOrderIdByRequestId(String requestId) {
        return Optional
                .ofNullable(routeMapper.selectOne(new QueryWrapper<PaymentRouteEntity>().eq("request_id", requestId)))
                .map(PaymentRouteEntity::getOrderId);
    }

    @Override
    public boolean updateStatus(long paymentId, PaymentStatus expectedStatus, PaymentOrder payment) {
        PaymentOrderEntity entity = toEntity(payment);
        return paymentOrderMapper.update(null,
                new UpdateWrapper<PaymentOrderEntity>().set("channel_trade_no", entity.getChannelTradeNo())
                        .set("status", entity.getStatus()).set("order_confirmed", entity.getOrderConfirmed())
                        .set("updated_at", entity.getUpdatedAt()).eq("payment_id", paymentId)
                        .eq("status", expectedStatus.name())) == 1;
    }

    @Override
    public boolean markOrderConfirmed(long paymentId, PaymentStatus expectedStatus, PaymentOrder payment) {
        PaymentOrderEntity entity = toEntity(payment);
        return paymentOrderMapper.update(null,
                new UpdateWrapper<PaymentOrderEntity>().set("order_confirmed", entity.getOrderConfirmed())
                        .set("updated_at", entity.getUpdatedAt()).eq("payment_id", paymentId)
                        .eq("status", expectedStatus.name()).eq("order_confirmed", false)) == 1;
    }

    @Override
    public List<PaymentOrder> findUnconfirmedByStatus(PaymentStatus status, int limit) {
        return paymentOrderMapper
                .selectList(new QueryWrapper<PaymentOrderEntity>().eq("status", status.name())
                        .eq("order_confirmed", false).orderByAsc("updated_at").last("LIMIT " + limit))
                .stream().map(this::toDomain).toList();
    }

    private PaymentOrderEntity toEntity(PaymentOrder payment) {
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setPaymentId(payment.paymentId());
        entity.setRequestId(payment.requestId());
        entity.setOrderId(payment.orderId());
        entity.setUserId(payment.userId());
        entity.setAmount(payment.amount());
        entity.setChannel(payment.channel());
        entity.setChannelTradeNo(payment.channelTradeNo());
        entity.setStatus(payment.status().name());
        entity.setOrderConfirmed(payment.orderConfirmed());
        entity.setCreatedAt(LocalDateTime.ofInstant(payment.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(payment.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private PaymentOrder toDomain(PaymentOrderEntity entity) {
        return new PaymentOrder(entity.getPaymentId(), entity.getRequestId(), entity.getOrderId(), entity.getUserId(),
                entity.getAmount(), entity.getChannel(), entity.getChannelTradeNo(),
                PaymentStatus.valueOf(entity.getStatus()), entity.getOrderConfirmed(),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
