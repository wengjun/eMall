package com.emall.cost.domain;

public enum CapacityRiskLevel {
    NONE,
    SCALE_OUT_REQUIRED,
    HPA_NEAR_LIMIT,
    IDLE_RESOURCE,
    OVER_REPLICATED
}
