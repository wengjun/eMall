package com.emall.common.sharding;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutedDataSource extends AbstractRoutingDataSource implements AutoCloseable {
    private List<? extends DataSource> managedDataSources = List.of();

    void manage(List<? extends DataSource> dataSources) {
        this.managedDataSources = List.copyOf(dataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return ShardContext.current().map(ShardRoutingDecision::databaseName).orElse(null);
    }

    @Override
    public void close() throws Exception {
        Exception failure = null;
        for (DataSource dataSource : managedDataSources) {
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
