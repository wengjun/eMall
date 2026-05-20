package com.emall.common.sharding;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutedDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return ShardContext.current().map(ShardRoutingDecision::databaseName).orElse(null);
    }
}
