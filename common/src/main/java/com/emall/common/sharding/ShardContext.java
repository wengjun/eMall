package com.emall.common.sharding;

import java.util.Optional;

public final class ShardContext {
    private static final ThreadLocal<ShardRoutingDecision> CURRENT = new ThreadLocal<>();

    private ShardContext() {
    }

    public static Optional<ShardRoutingDecision> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static ShardScope use(ShardRoutingDecision decision) {
        ShardRoutingDecision previous = CURRENT.get();
        CURRENT.set(decision);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public static String resolveTableName(String logicalTable) {
        return current().map(decision -> decision.resolveTableName(logicalTable)).orElse(logicalTable);
    }
}
