package com.emall.common.sharding;

import java.util.function.Supplier;

public interface ShardRoutingOperations {
    <T> T execute(String logicalTable, long shardKey, Supplier<T> action);

    <T> T execute(String logicalTable, String shardKey, Supplier<T> action);

    default <T> T executeRead(String logicalTable, long shardKey, Supplier<T> action) {
        return execute(logicalTable, shardKey, action);
    }

    default <T> T executeRead(String logicalTable, String shardKey, Supplier<T> action) {
        return execute(logicalTable, shardKey, action);
    }

    default <T> T executePhysicalShard(String logicalTable, int shardIndex, Supplier<T> action) {
        return action.get();
    }

    default int physicalShardCount(String logicalTable) {
        return 1;
    }

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
