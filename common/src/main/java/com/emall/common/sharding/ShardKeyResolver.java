package com.emall.common.sharding;

@FunctionalInterface
public interface ShardKeyResolver<T> {
    long resolveShardKey(T command);
}
