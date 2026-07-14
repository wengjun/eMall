package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ShardRoutingDataSourceIT {
    private static final String SHARD_ZERO = "jdbc:h2:mem:emall_order_00;DB_CLOSE_DELAY=-1";
    private static final String SHARD_ONE = "jdbc:h2:mem:emall_order_01;DB_CLOSE_DELAY=-1";

    @BeforeAll
    static void createPhysicalSchemas() {
        createProbe(SHARD_ZERO, "zero");
        createProbe(SHARD_ONE, "one");
    }

    @Test
    void selectsShardBeforeFirstConnectionInsideAnAlreadyStartedTransaction() {
        ShardRoutingProperties routing = new ShardRoutingProperties();
        routing.setEnabled(true);
        routing.setDatabasePrefix("emall_order");
        routing.setDatabaseShardCount(2);
        ShardDataSourceProperties dataSources = dataSources();
        DataSource routed = new ShardRoutingAutoConfiguration().routedDataSource(dataSources, routing);
        DefaultShardRoutingOperations operations = new DefaultShardRoutingOperations(routing);
        JdbcTemplate jdbc = new JdbcTemplate(routed);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(routed));

        String shardZero = transaction.execute(status -> operations.execute("route_probe", 0L,
                () -> jdbc.queryForObject("SELECT marker FROM route_probe", String.class)));
        String shardOne = transaction.execute(status -> operations.execute("route_probe", 1L,
                () -> jdbc.queryForObject("SELECT marker FROM route_probe", String.class)));

        assertThat(shardZero).isEqualTo("zero");
        assertThat(shardOne).isEqualTo("one");
    }

    private static void createProbe(String url, String marker) {
        JdbcTemplate jdbc = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(url));
        jdbc.execute("CREATE TABLE IF NOT EXISTS route_probe(marker VARCHAR(16) NOT NULL)");
        jdbc.update("DELETE FROM route_probe");
        jdbc.update("INSERT INTO route_probe(marker) VALUES (?)", marker);
    }

    private ShardDataSourceProperties dataSources() {
        ShardDataSourceProperties properties = new ShardDataSourceProperties();
        Map<String, ShardDataSourceProperties.DataSourceSpec> configured = new LinkedHashMap<>();
        configured.put("emall_order_00", spec(SHARD_ZERO));
        configured.put("emall_order_01", spec(SHARD_ONE));
        properties.setDatasources(configured);
        properties.setDefaultName("emall_order_00");
        return properties;
    }

    private ShardDataSourceProperties.DataSourceSpec spec(String jdbcUrl) {
        ShardDataSourceProperties.DataSourceSpec spec = new ShardDataSourceProperties.DataSourceSpec();
        spec.setJdbcUrl(jdbcUrl);
        spec.setMaximumPoolSize(2);
        return spec;
    }
}
