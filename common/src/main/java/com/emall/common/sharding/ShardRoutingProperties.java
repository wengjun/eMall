package com.emall.common.sharding;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("emall.sharding")
@Data
public class ShardRoutingProperties {
    private boolean enabled;
    private String databasePrefix = "emall";
    private int databaseShardCount = 1;
    private int logicalShardCount = 64;
    private String defaultCellId = "cell-a";
    private Map<Integer, String> shardCells = new LinkedHashMap<>();
    private Map<String, TableRule> tables = new LinkedHashMap<>();

    public TableRule tableRule(String logicalTable) {
        return tables.getOrDefault(logicalTable, new TableRule(logicalTable, 1));
    }

    public void setLogicalShardCount(int logicalShardCount) {
        if (logicalShardCount <= 0) {
            throw new IllegalArgumentException("logicalShardCount must be positive");
        }
        this.logicalShardCount = logicalShardCount;
    }

    @Data
    public static class TableRule {
        private String tablePrefix;
        private int tableShardCount = 1;

        public TableRule() {
        }

        public TableRule(String tablePrefix, int tableShardCount) {
            this.tablePrefix = tablePrefix;
            this.tableShardCount = tableShardCount;
        }
    }
}
