package com.emall.common.sharding;

import java.time.Duration;
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
    private int virtualShardCount = 4096;
    private String mappingNamespace;
    private Duration mappingCacheTtl = Duration.ofSeconds(30);
    private Duration staleReadTtl = Duration.ofHours(24);
    private Duration minimumCutoverDelay = Duration.ofSeconds(60);
    private String defaultRegionId = "default-region";
    private String defaultCellId = "cell-a";
    private Map<Integer, String> shardCells = new LinkedHashMap<>();
    private Map<String, TableRule> tables = new LinkedHashMap<>();

    public TableRule tableRule(String logicalTable) {
        return tables.getOrDefault(logicalTable, new TableRule(logicalTable, 1));
    }

    public String mappingNamespace() {
        return mappingNamespace == null || mappingNamespace.isBlank()
                ? databasePrefix.replace('_', '-')
                : mappingNamespace;
    }

    @Deprecated(forRemoval = false)
    public int getLogicalShardCount() {
        return virtualShardCount;
    }

    @Deprecated(forRemoval = false)
    public void setLogicalShardCount(int logicalShardCount) {
        if (logicalShardCount <= 0) {
            throw new IllegalArgumentException("logicalShardCount must be positive");
        }
        this.virtualShardCount = logicalShardCount;
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
