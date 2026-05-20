package com.emall.common.sharding;

import java.util.function.Supplier;

public interface ShardRoutingOperations {
    <T> T execute(String logicalTable, long shardKey, Supplier<T> action);

    <T> T execute(String logicalTable, String shardKey, Supplier<T> action);

    static ShardRoutingOperations noop() {
        return new ShardRoutingOperations() {
            @Override
            public <T> T execute(String logicalTable, long shardKey, Supplier<T> action) {
                return action.get();
            }

            @Override
            public <T> T execute(String logicalTable, String shardKey, Supplier<T> action) {
                return action.get();
            }
        };
    }
}
