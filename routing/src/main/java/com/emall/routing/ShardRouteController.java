package com.emall.routing;

import com.emall.common.api.ApiResponse;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.operations.InternalOperationAuthorizer;
import com.emall.common.sharding.ShardRouteCacheRebuildResult;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.sharding.ShardRoutePage;
import com.emall.common.sharding.ShardRouteRecord;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/shard-routes")
public class ShardRouteController {
    private final ShardRouteService service;
    private final ShardRouteIndex routeIndex;
    private final String operationsToken;

    public ShardRouteController(ShardRouteService service, ShardRouteIndex routeIndex,
            @Value("${emall.internal.operations-token}") String operationsToken) {
        this.service = service;
        this.routeIndex = routeIndex;
        this.operationsToken = operationsToken;
    }

    @GetMapping("/{namespace}/{lookupHash}")
    public ApiResponse<ShardRouteRecord> resolve(@PathVariable String namespace, @PathVariable String lookupHash,
            @RequestHeader("X-Internal-Token") String token) {
        authorize(token);
        return ApiResponse.ok(service.resolve(namespace, lookupHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "shard route not found")));
    }

    @PutMapping("/{namespace}/{lookupHash}")
    public ApiResponse<ShardRouteRecord> bind(@PathVariable String namespace, @PathVariable String lookupHash,
            @RequestHeader("X-Internal-Token") String token, @RequestBody BindRouteRequest request) {
        authorize(token);
        return ApiResponse
                .ok(service.bind(namespace, lookupHash, request.shardKey(), request.expiresAt(), request.unique()));
    }

    @DeleteMapping("/{namespace}/{lookupHash}")
    public ApiResponse<Boolean> remove(@PathVariable String namespace, @PathVariable String lookupHash,
            @RequestHeader("X-Internal-Token") String token, @RequestParam long shardKey,
            @RequestParam(required = false) Long expectedVersion) {
        authorize(token);
        return ApiResponse.ok(service.removeIfOwned(namespace, lookupHash, shardKey, expectedVersion));
    }

    @GetMapping
    public ApiResponse<ShardRoutePage> scan(@RequestHeader("X-Internal-Token") String token,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "1000") int limit) {
        authorize(token);
        return ApiResponse.ok(service.scan(cursor, limit));
    }

    @PostMapping("/cache/rebuild")
    public ApiResponse<ShardRouteCacheRebuildResult> rebuildCache(@RequestHeader("X-Internal-Token") String token,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "100000") int limit) {
        authorize(token);
        return ApiResponse.ok(routeIndex.rebuildCache(cursor, limit));
    }

    private void authorize(String token) {
        InternalOperationAuthorizer.requireAuthorized(operationsToken, token);
    }

    public record BindRouteRequest(long shardKey, Instant expiresAt, boolean unique) {
    }
}
