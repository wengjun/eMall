package com.emall.routing;

import com.emall.common.api.ApiResponse;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.operations.InternalOperationAuthorizer;
import com.emall.common.sharding.PhysicalShardPlacement;
import com.emall.common.sharding.ShardMigrationState;
import com.emall.common.sharding.VirtualShardPlacement;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/virtual-shards")
public class VirtualShardMigrationController {
    private final VirtualShardMigrationService service;
    private final String operationsToken;

    public VirtualShardMigrationController(VirtualShardMigrationService service,
            @Value("${emall.internal.operations-token}") String operationsToken) {
        this.service = service;
        this.operationsToken = operationsToken;
    }

    @GetMapping("/{namespace}/{virtualShard}")
    public ApiResponse<VirtualShardPlacement> resolve(@PathVariable String namespace, @PathVariable int virtualShard,
            @RequestHeader("X-Internal-Token") String token) {
        authorize(token);
        return ApiResponse.ok(service.resolve(namespace, virtualShard)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "virtual shard placement not found")));
    }

    @GetMapping
    public ApiResponse<List<VirtualShardPlacement>> list(@RequestParam String namespace,
            @RequestHeader("X-Internal-Token") String token) {
        authorize(token);
        return ApiResponse.ok(service.list(namespace));
    }

    @PostMapping("/{namespace}/{virtualShard}/start")
    public ApiResponse<VirtualShardPlacement> start(@PathVariable String namespace, @PathVariable int virtualShard,
            @RequestHeader("X-Internal-Token") String token, @RequestBody StartMigrationRequest request) {
        authorize(token);
        return ApiResponse.ok(service.start(namespace, virtualShard, request.source(), request.target(),
                request.expectedVersion(), request.operator()));
    }

    @PostMapping("/{namespace}/{virtualShard}/advance")
    public ApiResponse<VirtualShardPlacement> advance(@PathVariable String namespace, @PathVariable int virtualShard,
            @RequestHeader("X-Internal-Token") String token, @RequestBody AdvanceMigrationRequest request) {
        authorize(token);
        return ApiResponse.ok(service.advance(namespace, virtualShard, request.expectedVersion(), request.targetState(),
                request.evidence(), request.operator()));
    }

    @PostMapping("/{namespace}/{virtualShard}/fail")
    public ApiResponse<VirtualShardPlacement> fail(@PathVariable String namespace, @PathVariable int virtualShard,
            @RequestHeader("X-Internal-Token") String token, @RequestBody FailMigrationRequest request) {
        authorize(token);
        return ApiResponse.ok(
                service.fail(namespace, virtualShard, request.expectedVersion(), request.reason(), request.operator()));
    }

    @PostMapping("/{namespace}/{virtualShard}/rollback")
    public ApiResponse<VirtualShardPlacement> rollback(@PathVariable String namespace, @PathVariable int virtualShard,
            @RequestHeader("X-Internal-Token") String token, @RequestBody RollbackMigrationRequest request) {
        authorize(token);
        return ApiResponse.ok(service.rollback(namespace, virtualShard, request.expectedVersion(), request.operator()));
    }

    private void authorize(String token) {
        InternalOperationAuthorizer.requireAuthorized(operationsToken, token);
    }

    public record StartMigrationRequest(PhysicalShardPlacement source, PhysicalShardPlacement target,
            Long expectedVersion, String operator) {
    }

    public record AdvanceMigrationRequest(long expectedVersion, ShardMigrationState targetState,
            VirtualShardMigrationEvidence evidence, String operator) {
    }

    public record FailMigrationRequest(long expectedVersion, String reason, String operator) {
    }

    public record RollbackMigrationRequest(long expectedVersion, String operator) {
    }
}
