package com.emall.recommendation.domain;

import java.math.BigDecimal;

public record RecommendationItem(
        long skuId,
        String categoryCode,
        BigDecimal score,
        String strategyCode,
        String experimentBucket
) {
}
