package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class LazyShardDataSourceTest {
    @Test
    void shouldCreateAColdShardPoolOnlyOnFirstConnection() throws SQLException {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(connection);
        AtomicInteger creations = new AtomicInteger();
        LazyShardDataSource dataSource = new LazyShardDataSource(() -> {
            creations.incrementAndGet();
            return delegate;
        });

        assertThat(dataSource.initializedDataSource()).isEmpty();
        assertThat(dataSource.getConnection()).isSameAs(connection);
        assertThat(dataSource.getConnection()).isSameAs(connection);
        assertThat(creations).hasValue(1);
        verify(delegate, org.mockito.Mockito.times(2)).getConnection();
    }

    @Test
    void shouldNotInitializeAColdPoolDuringShutdown() throws Exception {
        AtomicInteger creations = new AtomicInteger();
        LazyShardDataSource dataSource = new LazyShardDataSource(() -> {
            creations.incrementAndGet();
            return mock(DataSource.class);
        });

        dataSource.close();

        assertThat(creations).hasValue(0);
        assertThatThrownBy(dataSource::getConnection).isInstanceOf(SQLException.class).hasMessageContaining("closed");
    }

    @Test
    void shouldInitializeOnlyOnePoolUnderConcurrentFirstTraffic() throws Exception {
        DataSource delegate = mock(DataSource.class);
        when(delegate.getConnection()).thenReturn(mock(Connection.class));
        AtomicInteger creations = new AtomicInteger();
        LazyShardDataSource dataSource = new LazyShardDataSource(() -> {
            creations.incrementAndGet();
            return delegate;
        });
        int callers = 32;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        try {
            var futures = java.util.stream.IntStream.range(0, callers).mapToObj(index -> executor.submit(() -> {
                ready.countDown();
                start.await();
                return dataSource.getConnection();
            })).toList();
            ready.await();
            start.countDown();
            for (var future : futures) {
                assertThat(future.get()).isNotNull();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(creations).hasValue(1);
    }

    @Test
    void shouldCloseAnInitializedPoolExactlyOnce() throws Exception {
        CloseableDataSource delegate = mock(CloseableDataSource.class);
        when(delegate.getConnection()).thenReturn(mock(Connection.class));
        LazyShardDataSource dataSource = new LazyShardDataSource(() -> delegate);

        dataSource.getConnection();
        dataSource.close();
        dataSource.close();

        verify(delegate).close();
        assertThatThrownBy(dataSource::getConnection).isInstanceOf(SQLException.class).hasMessageContaining("closed");
    }

    private interface CloseableDataSource extends DataSource, AutoCloseable {
        @Override
        void close();
    }
}
