package com.emall.recommendation.domain;

import java.math.BigDecimal;

public record RankingStrategy(
        String strategyCode,
        BigDecimal baseWeight,
        BigDecimal popularityWeight,
        BigDecimal affinityWeight
) {
    public static RankingStrategy balanced() {
        return new RankingStrategy("balanced", new BigDecimal("0.40"), new BigDecimal("0.30"),
                new BigDecimal("0.30"));
    }

    public static RankingStrategy popularity() {
        return new RankingStrategy("popularity", new BigDecimal("0.20"), new BigDecimal("0.60"),
                new BigDecimal("0.20"));
    }
}
