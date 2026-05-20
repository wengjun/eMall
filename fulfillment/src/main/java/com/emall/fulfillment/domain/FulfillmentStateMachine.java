package com.emall.fulfillment.domain;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class FulfillmentStateMachine {
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = new EnumMap<>(ShipmentStatus.class);

    static {
        ALLOWED.put(ShipmentStatus.ALLOCATED, EnumSet.of(ShipmentStatus.SHIPPED, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.SHIPPED, EnumSet.of(ShipmentStatus.DELIVERED));
        ALLOWED.put(ShipmentStatus.DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.CANCELLED, EnumSet.noneOf(ShipmentStatus.class));
    }

    private FulfillmentStateMachine() {
    }

    public static void requireTransition(ShipmentStatus current, ShipmentStatus next) {
        if (current == next) {
            return;
        }
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "illegal fulfillment status transition " + current + " -> " + next);
        }
    }
}
