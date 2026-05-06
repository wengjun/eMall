package com.emall.chaos;

public record AbortCondition(
        String metric,
        String operator,
        double threshold,
        int consecutiveMinutes
) {
}
