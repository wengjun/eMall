package com.emall.common.sharding;

@FunctionalInterface
public interface ShardScope extends AutoCloseable {
    @Override
    void close();
}
