package com.emall.common.sharding;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class DefaultShardRoutingOperations implements ShardRoutingOperations {
    private final ShardRoutingProperties properties;

    public DefaultShardRoutingOperations(ShardRoutingProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public <T> T execute(String logicalTable, long shardKey, Supplier<T> action) {
        if (!properties.isEnabled()) {
            return action.get();
        }
        ShardRoutingDecision decision = decide(logicalTable, shardKey);
        try (ShardScope ignored = ShardContext.use(decision)) {
            return action.get();
        }
    }

    @Override
    public <T> T execute(String logicalTable, String shardKey, Supplier<T> action) {
        return execute(logicalTable, Objects.requireNonNull(shardKey, "shardKey must not be null").hashCode(), action);
    }

    public ShardRoutingDecision decide(String logicalTable, long shardKey) {
        int logicalShard = Math.floorMod(Long.hashCode(shardKey), properties.getLogicalShardCount());
        ShardRoute primaryRoute = route(logicalTable, shardKey);
        Map<String, String> physicalTables = new LinkedHashMap<>();
        for (String table : properties.getTables().keySet()) {
            physicalTables.put(table, route(table, shardKey).tableName());
        }
        physicalTables.putIfAbsent(logicalTable, primaryRoute.tableName());
        String cellId = properties.getShardCells().getOrDefault(logicalShard, properties.getDefaultCellId());
        return new ShardRoutingDecision(logicalTable, shardKey, logicalShard, cellId, primaryRoute.databaseName(),
                primaryRoute.databaseIndex(), physicalTables);
    }

    private ShardRoute route(String logicalTable, long shardKey) {
        ShardRoutingProperties.TableRule rule = properties.tableRule(logicalTable);
        HashModShardRouter router = new HashModShardRouter(properties.getDatabasePrefix(),
                properties.getDatabaseShardCount(), rule.getTablePrefix(), rule.getTableShardCount());
        return router.route(shardKey);
    }
}
