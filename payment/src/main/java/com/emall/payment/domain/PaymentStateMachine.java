package com.emall.payment.domain;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PaymentStateMachine {
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED.put(PaymentStatus.CREATED, EnumSet.of(PaymentStatus.SUCCEEDED, PaymentStatus.CLOSED));
        ALLOWED.put(PaymentStatus.SUCCEEDED, EnumSet.of(PaymentStatus.REFUNDING, PaymentStatus.REFUNDED));
        ALLOWED.put(PaymentStatus.REFUNDING, EnumSet.of(PaymentStatus.REFUNDED));
        ALLOWED.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(PaymentStatus.CLOSED, EnumSet.noneOf(PaymentStatus.class));
    }

    private PaymentStateMachine() {
    }

    public static void requireTransition(PaymentStatus current, PaymentStatus next) {
        if (current == next) {
            return;
        }
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "illegal payment status transition " + current + " -> " + next);
        }
    }
}
