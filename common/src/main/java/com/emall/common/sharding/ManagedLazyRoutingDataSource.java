package com.emall.common.sharding;

import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

final class ManagedLazyRoutingDataSource extends LazyConnectionDataSourceProxy implements AutoCloseable {
    private final RoutedDataSource routedDataSource;

    ManagedLazyRoutingDataSource(RoutedDataSource routedDataSource) {
        super(routedDataSource);
        this.routedDataSource = routedDataSource;
    }

    @Override
    public void close() throws Exception {
        routedDataSource.close();
    }
}
