package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutedDataSourceTest {
    private final ExposedRoutedDataSource dataSource = new ExposedRoutedDataSource();

    @Test
    void shouldReturnNullLookupKeyWithoutShardContext() {
        assertThat(dataSource.lookupKey()).isNull();
    }

    @Test
    void shouldUseDatabaseNameFromCurrentShardDecision() {
        ShardRoutingDecision decision = new ShardRoutingDecision("order_record", 70001L, 1, "cell-a", "emall_order_01",
                1, Map.of("order_record", "order_record_01"));

        try (ShardScope ignored = ShardContext.use(decision)) {
            assertThat(dataSource.lookupKey()).isEqualTo("emall_order_01");
        }

        assertThat(dataSource.lookupKey()).isNull();
    }

    private static final class ExposedRoutedDataSource extends RoutedDataSource {
        private Object lookupKey() {
            return determineCurrentLookupKey();
        }
    }
}
