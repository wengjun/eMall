package com.emall.chaos;

public enum DrillType {
    REDIS_FAILURE,
    MQ_BACKLOG,
    DATABASE_FAILOVER,
    DOWNSTREAM_LATENCY,
    PARTIAL_REGION_ISOLATION
}
