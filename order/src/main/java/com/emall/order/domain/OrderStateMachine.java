package com.emall.order.domain;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.CREATED,
                EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.PENDING_RETRY));
        ALLOWED.put(OrderStatus.PENDING_RETRY,
                EnumSet.of(OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.PENDING_RETRY));
        ALLOWED.put(OrderStatus.PAID, EnumSet.of(OrderStatus.CLOSED));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CLOSED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStateMachine() {
    }

    public static void requireTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "illegal order status transition " + current + " -> " + next);
        }
    }
}
