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
    private Map<String, DataSourceSpec> datasources = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class DataSourceSpec {
        private String jdbcUrl;
        private String username;
        private String password;
        private int maximumPoolSize = 32;
        private long connectionTimeoutMs = 3000;
        private long validationTimeoutMs = 1000;
    }
}
