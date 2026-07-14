package com.emall.order.payment;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusOrderPaymentConfirmationRepository implements OrderPaymentConfirmationRepository {
    private final OrderPaymentConfirmationMapper mapper;

    MybatisPlusOrderPaymentConfirmationRepository(OrderPaymentConfirmationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrderPaymentConfirmation saveIfAbsent(OrderPaymentConfirmation confirmation) {
        try {
            mapper.insert(toEntity(confirmation));
            return confirmation;
        } catch (DuplicateKeyException ex) {
            return findByOrderId(confirmation.orderId()).orElseThrow(() -> ex);
        }
    }

    @Override
    public Optional<OrderPaymentConfirmation> findByOrderId(long orderId) {
        return Optional.ofNullable(mapper.selectById(orderId)).map(this::toDomain);
    }

    private OrderPaymentConfirmationEntity toEntity(OrderPaymentConfirmation confirmation) {
        OrderPaymentConfirmationEntity entity = new OrderPaymentConfirmationEntity();
        entity.setOrderId(confirmation.orderId());
        entity.setPaymentId(confirmation.paymentId());
        entity.setPaidAmount(confirmation.paidAmount());
        entity.setCurrency(confirmation.currency());
        entity.setChannelTradeNo(confirmation.channelTradeNo());
        entity.setConfirmedAt(LocalDateTime.ofInstant(confirmation.confirmedAt(), ZoneOffset.UTC));
        return entity;
    }

    private OrderPaymentConfirmation toDomain(OrderPaymentConfirmationEntity entity) {
        return new OrderPaymentConfirmation(entity.getOrderId(), entity.getPaymentId(), entity.getPaidAmount(),
                entity.getCurrency(), entity.getChannelTradeNo(), entity.getConfirmedAt().toInstant(ZoneOffset.UTC));
    }
}
