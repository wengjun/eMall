package com.emall.order.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
import com.emall.order.domain.OrderClientType;
import com.emall.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusOrderRepository implements OrderRepository {
    private final OrderMapper orderMapper;

    public MybatisPlusOrderRepository(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        try {
            orderMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            int updated = orderMapper.update(null, new UpdateWrapper<OrderEntity>()
                    .set("unit_price", entity.getUnitPrice())
                    .set("client_type", entity.getClientType())
                    .set("device_id", entity.getDeviceId())
                    .set("channel", entity.getChannel())
                    .set("subtotal_amount", entity.getSubtotalAmount())
                    .set("discount_amount", entity.getDiscountAmount())
                    .set("payable_amount", entity.getPayableAmount())
                    .set("currency", entity.getCurrency())
                    .set("price_version", entity.getPriceVersion())
                    .set("coupon_id", entity.getCouponId())
                    .set("inventory_reservation_id", entity.getInventoryReservationId())
                    .set("status", entity.getStatus())
                    .set("failure_reason", entity.getFailureReason())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("order_id", entity.getOrderId()));
            if (updated == 0) {
                return findByRequestId(order.requestId()).orElseThrow(() -> ex);
            }
        }
        return order;
    }

    @Override
    public Optional<Order> findById(long orderId) {
        return Optional.ofNullable(orderMapper.selectById(orderId)).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByRequestId(String requestId) {
        return Optional.ofNullable(orderMapper.selectOne(new QueryWrapper<OrderEntity>()
                .eq("request_id", requestId))).map(this::toDomain);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int limit) {
        return orderMapper.selectList(new QueryWrapper<OrderEntity>()
                .eq("status", status.name())
                .orderByAsc("updated_at")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    private OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.orderId());
        entity.setRequestId(order.requestId());
        entity.setUserId(order.userId());
        entity.setSkuId(order.skuId());
        entity.setQuantity(order.quantity());
        OrderClientContext clientContext = OrderClientContext.of(order.clientType(), order.deviceId(), order.channel());
        entity.setClientType(clientContext.clientType().name());
        entity.setDeviceId(clientContext.deviceId());
        entity.setChannel(clientContext.channel());
        entity.setUnitPrice(order.unitPrice());
        entity.setSubtotalAmount(order.subtotalAmount());
        entity.setDiscountAmount(order.discountAmount());
        entity.setPayableAmount(order.payableAmount());
        entity.setCurrency(order.currency());
        entity.setPriceVersion(order.priceVersion());
        entity.setCouponId(order.couponId());
        entity.setInventoryReservationId(order.inventoryReservationId());
        entity.setStatus(order.status().name());
        entity.setFailureReason(order.failureReason());
        entity.setCreatedAt(LocalDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(order.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Order toDomain(OrderEntity entity) {
        OrderClientContext clientContext =
                OrderClientContext.of(toClientType(entity.getClientType()), entity.getDeviceId(), entity.getChannel());
        return new Order(entity.getOrderId(), entity.getRequestId(), entity.getUserId(), entity.getSkuId(),
                entity.getQuantity(), clientContext.clientType(), clientContext.deviceId(), clientContext.channel(),
                entity.getUnitPrice(), entity.getSubtotalAmount(), entity.getDiscountAmount(),
                entity.getPayableAmount(), entity.getCurrency(), entity.getPriceVersion(), entity.getCouponId(),
                entity.getInventoryReservationId(), OrderStatus.valueOf(entity.getStatus()),
                entity.getFailureReason(), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private OrderClientType toClientType(String clientType) {
        if (clientType == null || clientType.isBlank()) {
            return OrderClientType.WEB;
        }
        return OrderClientType.valueOf(clientType);
    }
}
