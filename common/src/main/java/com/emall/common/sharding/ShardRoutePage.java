package com.emall.common.sharding;

import java.util.List;

public record ShardRoutePage(List<ShardRouteRecord> routes, String nextCursor) {
    public ShardRoutePage {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }
}
