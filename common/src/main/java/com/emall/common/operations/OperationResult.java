package com.emall.common.operations;

import java.time.Instant;

public record OperationResult(
        String operation,
        int affected,
        Instant executedAt
) {
    public static OperationResult of(String operation, int affected) {
        return new OperationResult(operation, affected, Instant.now());
    }
}
