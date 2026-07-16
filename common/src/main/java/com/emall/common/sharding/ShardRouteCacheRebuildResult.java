package com.emall.common.sharding;

public record ShardRouteCacheRebuildResult(int rebuilt, String nextCursor, boolean complete) {
}
