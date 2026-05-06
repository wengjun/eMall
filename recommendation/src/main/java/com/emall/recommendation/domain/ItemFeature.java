package com.emall.recommendation.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemFeature(
        long skuId,
        String categoryCode,
        BigDecimal baseScore,
        BigDecimal popularityScore,
        boolean active,
        Instant updatedAt
) {
}
