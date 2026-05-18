package com.emall.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientType;
import com.emall.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

class MybatisPlusOrderRepositoryTest {
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final MybatisPlusOrderRepository repository = new MybatisPlusOrderRepository(orderMapper);

    @Test
    void shouldPersistOrderClientType() {
        Order order = newOrder(10001L, "web-request-001", OrderClientType.WEB);

        repository.save(order);

        ArgumentCaptor<OrderEntity> entityCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getClientType()).isEqualTo("WEB");
    }

    @Test
    void shouldUpdateExistingOrderByOrderIdWhenPrimaryKeyAlreadyExists() {
        Order order = newOrder(10001L, "app-request-001", OrderClientType.APP);
        when(orderMapper.insert(any(OrderEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(orderMapper.update(isNull(), anyWrapper())).thenReturn(1);

        Order saved = repository.save(order);

        assertThat(saved).isEqualTo(order);
        verify(orderMapper, never()).selectOne(anyWrapper());
    }

    @Test
    void shouldReturnExistingOrderWhenDuplicateRequestRaceLoses() {
        Order existing = newOrder(10001L, "shared-request-001", OrderClientType.APP);
        Order competingOrder = newOrder(10002L, "shared-request-001", OrderClientType.WEB);
        when(orderMapper.insert(any(OrderEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(orderMapper.update(isNull(), anyWrapper())).thenReturn(0);
        when(orderMapper.selectOne(anyWrapper())).thenReturn(toEntity(existing));

        Order saved = repository.save(competingOrder);

        assertThat(saved.orderId()).isEqualTo(existing.orderId());
        assertThat(saved.clientType()).isEqualTo(OrderClientType.APP);
    }

    @Test
    void shouldDefaultMissingClientTypeToWebWhenReadingLegacyRows() {
        OrderEntity entity = toEntity(newOrder(10001L, "legacy-request-001", OrderClientType.WEB));
        entity.setClientType(null);
        when(orderMapper.selectById(10001L)).thenReturn(entity);

        Order order = repository.findById(10001L).orElseThrow();

        assertThat(order.clientType()).isEqualTo(OrderClientType.WEB);
    }

    private static Order newOrder(long orderId, String requestId, OrderClientType clientType) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new Order(orderId, requestId, 70001L, 30001L, 1, clientType, new BigDecimal("100.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "CNY", 1L, null,
                "order-" + orderId, OrderStatus.CREATED, null, now, now);
    }

    private static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.orderId());
        entity.setRequestId(order.requestId());
        entity.setUserId(order.userId());
        entity.setSkuId(order.skuId());
        entity.setQuantity(order.quantity());
        entity.setClientType(order.clientType().name());
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

    @SuppressWarnings("unchecked")
    private Wrapper<OrderEntity> anyWrapper() {
        return any(Wrapper.class);
    }
}
