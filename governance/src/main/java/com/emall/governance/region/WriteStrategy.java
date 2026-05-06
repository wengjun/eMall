package com.emall.governance.region;

public enum WriteStrategy {
    GLOBAL_SINGLE_WRITER,
    PARTITIONED_SINGLE_WRITER,
    ACTIVE_ACTIVE_READ_LOCAL
}
