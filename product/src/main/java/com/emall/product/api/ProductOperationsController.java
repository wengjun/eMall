package com.emall.product.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.operations.InternalOperationsControllerSupport;
import com.emall.common.operations.OperationAuditRepository;
import com.emall.common.operations.OperationResult;
import com.emall.product.messaging.OutboxPublisher;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/operations")
public class ProductOperationsController extends InternalOperationsControllerSupport {
    private final OutboxPublisher outboxPublisher;

    public ProductOperationsController(OutboxPublisher outboxPublisher,
            OperationAuditRepository operationAuditRepository,
            @Value("${emall.internal.operations-token}") String operationsToken) {
        super(operationAuditRepository, "product", operationsToken);
        this.outboxPublisher = outboxPublisher;
    }

    @PostMapping("/outbox/publish")
    public ApiResponse<OperationResult> publishOutbox(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        return execute(token, operator, traceId, "outbox.publish", () -> outboxPublisher.publishBatch(limit));
    }

    @PostMapping("/outbox/retry-failed")
    public ApiResponse<OperationResult> retryFailedOutbox(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        return execute(token, operator, traceId, "outbox.retry-failed", () -> outboxPublisher.retryFailedNow(limit));
    }
}
