package com.emall.search.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.operations.InternalOperationsControllerSupport;
import com.emall.common.operations.OperationAuditRepository;
import com.emall.common.operations.OperationResult;
import com.emall.search.service.SearchIndexLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.function.IntSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/operations/search-index")
public class SearchIndexOperationsController extends InternalOperationsControllerSupport {
    private final SearchIndexLifecycleService lifecycleService;

    public SearchIndexOperationsController(SearchIndexLifecycleService lifecycleService,
            OperationAuditRepository operationAuditRepository,
            @Value("${emall.internal.operations-token}") String operationsToken,
            @Value("${emall.internal.require-approval:true}") boolean approvalRequired) {
        super(operationAuditRepository, "search", operationsToken, approvalRequired);
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/prepare")
    public ApiResponse<OperationResult> prepare(
            @RequestParam @Pattern(regexp = "[a-z0-9][a-z0-9._-]{0,63}") String version,
            @RequestParam(defaultValue = "24") @Min(1) @Max(256) int shards,
            @RequestParam(defaultValue = "1") @Min(0) @Max(8) int replicas, HttpServletRequest request) {
        return execute(request, "version=" + version + ",shards=" + shards + ",replicas=" + replicas,
                "search-index.prepare", () -> lifecycleService.prepare(version, shards, replicas));
    }

    @PostMapping("/reindex")
    public ApiResponse<OperationResult> reindex(
            @RequestParam @Pattern(regexp = "[a-z0-9][a-z0-9._-]{0,63}") String version,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(1_000_000) long requestsPerSecond,
            HttpServletRequest request) {
        return execute(request, "version=" + version + ",requestsPerSecond=" + requestsPerSecond,
                "search-index.reindex", () -> lifecycleService.reindex(version, requestsPerSecond));
    }

    @PostMapping("/verify")
    public ApiResponse<OperationResult> verifyIndex(
            @RequestParam @Pattern(regexp = "[a-z0-9][a-z0-9._-]{0,63}") String version,
            @RequestParam(defaultValue = "-1") @Min(-1) long expectedCount, HttpServletRequest request) {
        return execute(request, "version=" + version + ",expectedCount=" + expectedCount, "search-index.verify",
                () -> lifecycleService.verify(version, expectedCount));
    }

    @PostMapping("/activate")
    public ApiResponse<OperationResult> activate(
            @RequestParam @Pattern(regexp = "[a-z0-9][a-z0-9._-]{0,63}") String version,
            @RequestParam(defaultValue = "-1") @Min(-1) long expectedCount, HttpServletRequest request) {
        return execute(request, "version=" + version + ",expectedCount=" + expectedCount, "search-index.activate",
                () -> lifecycleService.activate(version, expectedCount));
    }

    @PostMapping("/rollback")
    public ApiResponse<OperationResult> rollback(
            @RequestParam @Pattern(regexp = "[a-z0-9][a-z0-9._-]{0,63}") String version,
            @RequestParam @Min(0) long expectedCount, HttpServletRequest request) {
        return execute(request, "version=" + version + ",expectedCount=" + expectedCount, "search-index.rollback",
                () -> lifecycleService.activate(version, expectedCount));
    }

    private ApiResponse<OperationResult> execute(HttpServletRequest request, String parameterDigest, String operation,
            IntSupplier action) {
        return execute(request.getHeader("X-Internal-Token"), header(request, "X-Operator", "unknown"),
                request.getHeader("X-Trace-Id"), header(request, "X-Operator-Role", "ops-admin"),
                request.getHeader("X-Approval-Id"), request.getHeader("X-Source-Identity"), parameterDigest, operation,
                action);
    }

    private String header(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
