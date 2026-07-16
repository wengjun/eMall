package com.emall.common.sharding;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("emall.sharding.datasource")
public class ShardDataSourceProperties {
    private boolean enabled;
    private String defaultName;
    private String jdbcUrlTemplate;
    private String username;
    private String password;
    private int defaultMaximumPoolSize = 4;
    private int defaultMinimumIdle;
    private int podConnectionBudget = 64;
    private int plannedMaxReplicas = 10;
    private int databaseInstanceConnectionBudget = 50;
    private int globalConnectionBudget = 800;
    private int connectionHeadroomPercent = 20;
    private long idleTimeoutMs = 60000;
    private long maxLifetimeMs = 1800000;
    private boolean lazyInitialization = true;
    private Map<String, DataSourceSpec> datasources = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class DataSourceSpec {
        private String jdbcUrl;
        private String username;
        private String password;
        private Integer maximumPoolSize;
        private Integer minimumIdle;
        private long connectionTimeoutMs = 3000;
        private long validationTimeoutMs = 1000;
    }
}
