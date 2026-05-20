package com.emall.payment.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.operations.InternalOperationsControllerSupport;
import com.emall.common.operations.OperationAuditRepository;
import com.emall.common.operations.OperationResult;
import com.emall.payment.domain.StatementType;
import com.emall.payment.job.PaymentCompensationJob;
import com.emall.payment.messaging.OutboxPublisher;
import com.emall.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/operations")
public class PaymentOperationsController extends InternalOperationsControllerSupport {
    private final PaymentCompensationJob paymentCompensationJob;
    private final OutboxPublisher outboxPublisher;
    private final PaymentService paymentService;

    public PaymentOperationsController(PaymentCompensationJob paymentCompensationJob, OutboxPublisher outboxPublisher,
            PaymentService paymentService, OperationAuditRepository operationAuditRepository,
            @Value("${emall.internal.operations-token}") String operationsToken,
            @Value("${emall.internal.require-approval:false}") boolean approvalRequired) {
        super(operationAuditRepository, "payment", operationsToken, approvalRequired);
        this.paymentCompensationJob = paymentCompensationJob;
        this.outboxPublisher = outboxPublisher;
        this.paymentService = paymentService;
    }

    @PostMapping("/payments/retry-order-confirmation")
    public ApiResponse<OperationResult> retryOrderConfirmation(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Operator-Role", defaultValue = "ops-admin") String role,
            @RequestHeader(value = "X-Approval-Id", required = false) String approvalId,
            @RequestHeader(value = "X-Source-Identity", required = false) String sourceIdentity) {
        return execute(token, operator, traceId, role, approvalId, sourceIdentity, "limit=" + limit,
                "payments.retry-order-confirmation", () -> paymentCompensationJob.retryOrderConfirmation(limit));
    }

    @PostMapping("/payments/channel-statements")
    public ApiResponse<OperationResult> ingestChannelStatement(@Valid @RequestBody ChannelStatementRequest request,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Operator-Role", defaultValue = "ops-admin") String role,
            @RequestHeader(value = "X-Approval-Id", required = false) String approvalId,
            @RequestHeader(value = "X-Source-Identity", required = false) String sourceIdentity) {
        return execute(token, operator, traceId, role, approvalId, sourceIdentity, "paymentId=" + request.paymentId(),
                "payments.ingest-channel-statement", () -> {
                    paymentService.ingestChannelStatement(request.channel(), request.channelTradeNo(),
                            request.paymentId(), request.amount(), request.statementType(), request.occurredAt());
                    return 1;
                });
    }

    @PostMapping("/payments/reconcile-channel-statements")
    public ApiResponse<OperationResult> reconcileChannelStatements(
            @RequestParam(defaultValue = "100") @Positive @Max(1000) int limit,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Operator", defaultValue = "unknown") String operator,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Operator-Role", defaultValue = "ops-admin") String role,
            @RequestHeader(value = "X-Approval-Id", required = false) String approvalId,
            @RequestHeader(value = "X-Source-Identity", required = false) String sourceIdentity) {
        return execute(token, operator, traceId, role, approvalId, sourceIdentity, "limit=" + limit,
                "payments.reconcile-channel-statements",
                () -> paymentCompensationJob.reconcileChannelStatements(limit));
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

    public record ChannelStatementRequest(@NotBlank String channel, @NotBlank String channelTradeNo,
            @Positive long paymentId, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull StatementType statementType, @NotNull Instant occurredAt) {
    }
}
