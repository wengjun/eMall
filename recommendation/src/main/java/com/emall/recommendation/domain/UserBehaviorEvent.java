package com.emall.recommendation.domain;

import java.time.Instant;

public record UserBehaviorEvent(
        long eventId,
        long userId,
        long skuId,
        String categoryCode,
        BehaviorType behaviorType,
        int weight,
        Instant occurredAt
) {
}
