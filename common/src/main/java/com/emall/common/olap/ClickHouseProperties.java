package com.emall.common.olap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "emall.olap")
@Getter
@Setter
public class ClickHouseProperties {
    private String engine = "jdbc";
    private String clickhouseUrl = "jdbc:clickhouse://localhost:8123/emall";
    private String username = "default";
    private String password = "";
}
