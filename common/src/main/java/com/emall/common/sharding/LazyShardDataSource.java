package com.emall.common.sharding;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.sql.DataSource;

final class LazyShardDataSource implements DataSource, AutoCloseable {
    private final Supplier<? extends DataSource> factory;
    private final AtomicReference<DataSource> delegate = new AtomicReference<>();
    private volatile boolean closed;

    LazyShardDataSource(Supplier<? extends DataSource> factory) {
        this.factory = factory;
    }

    Optional<DataSource> initializedDataSource() {
        return Optional.ofNullable(delegate.get());
    }

    void initialize() throws SQLException {
        delegate();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate().getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        try {
            return delegate().getParentLogger();
        } catch (SQLException exception) {
            throw new SQLFeatureNotSupportedException("cannot initialize shard datasource", exception);
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate().isWrapperFor(iface);
    }

    @Override
    public void close() throws Exception {
        DataSource current;
        synchronized (delegate) {
            if (closed) {
                return;
            }
            closed = true;
            current = delegate.getAndSet(null);
        }
        if (current instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    private DataSource delegate() throws SQLException {
        ensureOpen();
        DataSource current = delegate.get();
        if (current != null) {
            return current;
        }
        synchronized (delegate) {
            ensureOpen();
            current = delegate.get();
            if (current == null) {
                current = factory.get();
                delegate.set(current);
            }
        }
        return current;
    }

    private void ensureOpen() throws SQLException {
        if (closed) {
            throw new SQLException("shard datasource is closed");
        }
    }
}
