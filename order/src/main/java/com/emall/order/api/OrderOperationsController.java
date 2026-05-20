package com.emall.order.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.operations.InternalOperationsControllerSupport;
import com.emall.common.operations.OperationAuditRepository;
import com.emall.common.operations.OperationResult;
import com.emall.order.job.OrderCompensationJob;
import com.emall.order.messaging.OutboxPublisher;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/operations")
public class OrderOperationsController extends InternalOperationsControllerSupport {
    private final OrderCompensationJob orderCompensationJob;
    private final OutboxPublisher outboxPublisher;

    public OrderOperationsController(OrderCompensationJob orderCompensationJob, OutboxPublisher outboxPublisher,
            OperationAuditRepository operationAuditRepository,
            @Value("${emall.internal.operations-token}") String operationsToken,
            @Value("${emall.internal.require-approval:false}") boolean approvalRequired) {
        super(operationAuditRepository, "order", operationsToken, approvalRequired);
        this.orderCompensationJob = orderCompensationJob;
        this.outboxPublisher = outboxPublisher;
    }

    @PostMapping("/orders/retry-pending")
    public ApiResponse<OperationResult> retryPendingOrders(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Operator-Role", defaultValue = "ops-admin") String role,
            @RequestHeader(value = "X-Approval-Id", required = false) String approvalId,
            @RequestHeader(value = "X-Source-Identity", required = false) String sourceIdentity) {
        return execute(token, operator, traceId, role, approvalId, sourceIdentity, "limit=" + limit,
                "orders.retry-pending", () -> orderCompensationJob.retryPendingOrders(limit));
    }

    @PostMapping("/outbox/publish")
    public ApiResponse<OperationResult> publishOutbox(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Operator-Role", defaultValue = "ops-admin") String role,
            @RequestHeader(value = "X-Approval-Id", required = false) String approvalId,
            @RequestHeader(value = "X-Source-Identity", required = false) String sourceIdentity) {
        return execute(token, operator, traceId, role, approvalId, sourceIdentity, "limit=" + limit, "outbox.publish",
                () -> outboxPublisher.publishBatch(limit));
    }

    @PostMapping("/outbox/retry-failed")
    public ApiResponse<OperationResult> retryFailedOutbox(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Operator-Role", defaultValue = "ops-admin") String role,
            @RequestHeader(value = "X-Approval-Id", required = false) String approvalId,
            @RequestHeader(value = "X-Source-Identity", required = false) String sourceIdentity) {
        return execute(token, operator, traceId, role, approvalId, sourceIdentity, "limit=" + limit,
                "outbox.retry-failed", () -> outboxPublisher.retryFailedNow(limit));
    }
}
