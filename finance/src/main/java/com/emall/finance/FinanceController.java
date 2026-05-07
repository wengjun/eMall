package com.emall.finance;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
class FinanceController {
    private final FinanceService financeService;

    FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/accounts")
    ApiResponse<FinanceAccount> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse
                .ok(financeService.createAccount(request.accountType(), request.ownerId(), request.currency()));
    }

    @PostMapping("/ledger-entries")
    ApiResponse<LedgerEntry> postEntry(@Valid @RequestBody PostLedgerEntryRequest request) {
        return ApiResponse.ok(financeService.postEntry(request.accountId(), request.businessType(),
                request.businessNo(), request.debitAmount(), request.creditAmount()));
    }

    @PostMapping("/accounts/{accountId}/freeze")
    ApiResponse<FinanceAccount> freezeFunds(@PathVariable long accountId,
            @Valid @RequestBody FreezeFundsRequest request) {
        return ApiResponse.ok(financeService.freezeFunds(accountId, request.amount()));
    }

    @PostMapping("/settlement-batches")
    ApiResponse<SettlementBatch> createSettlementBatch(@Valid @RequestBody CreateSettlementBatchRequest request) {
        return ApiResponse.ok(financeService.createSettlementBatch(request.merchantId(), request.amount(),
                request.commissionAmount(), request.settlementDate()));
    }

    @PatchMapping("/settlement-batches/{batchId}/status")
    ApiResponse<SettlementBatch> changeSettlementStatus(@PathVariable long batchId,
            @Valid @RequestBody ChangeSettlementStatusRequest request) {
        return ApiResponse.ok(financeService.changeSettlementStatus(batchId, request.status()));
    }

    @PostMapping("/invoices")
    ApiResponse<InvoiceDocument> issueInvoice(@Valid @RequestBody IssueInvoiceRequest request) {
        return ApiResponse.ok(financeService.issueInvoice(request.ownerId(), request.amount(), request.taxNo()));
    }

    @PatchMapping("/invoices/{invoiceId}/status")
    ApiResponse<InvoiceDocument> reconcileInvoice(@PathVariable long invoiceId,
            @Valid @RequestBody ReconcileInvoiceRequest request) {
        return ApiResponse.ok(financeService.reconcileInvoice(invoiceId, request.status()));
    }

    @PostMapping("/clearing-files")
    ApiResponse<ClearingFile> recordClearingFile(@Valid @RequestBody RecordClearingFileRequest request) {
        return ApiResponse.ok(financeService.recordClearingFile(request.channel(), request.clearingDate(),
                request.amount(), request.balanced()));
    }

    @PostMapping("/chargebacks")
    ApiResponse<ChargebackCase> openChargeback(@Valid @RequestBody OpenChargebackRequest request) {
        return ApiResponse.ok(financeService.openChargeback(request.paymentId(), request.amount(), request.reason()));
    }

    @GetMapping("/summary")
    ApiResponse<FinanceSummary> summary() {
        return ApiResponse.ok(financeService.summary());
    }

    record CreateAccountRequest(AccountType accountType, @Positive long ownerId, @NotBlank String currency) {
    }

    record PostLedgerEntryRequest(@Positive long accountId, @NotBlank String businessType, @NotBlank String businessNo,
            BigDecimal debitAmount, BigDecimal creditAmount) {
    }

    record FreezeFundsRequest(@DecimalMin("0.01") BigDecimal amount) {
    }

    record CreateSettlementBatchRequest(@Positive long merchantId, @DecimalMin("0.01") BigDecimal amount,
            @DecimalMin("0.00") BigDecimal commissionAmount, LocalDate settlementDate) {
    }

    record ChangeSettlementStatusRequest(SettlementStatus status) {
    }

    record IssueInvoiceRequest(@Positive long ownerId, @DecimalMin("0.01") BigDecimal amount, @NotBlank String taxNo) {
    }

    record ReconcileInvoiceRequest(InvoiceStatus status) {
    }

    record RecordClearingFileRequest(@NotBlank String channel, LocalDate clearingDate,
            @DecimalMin("0.01") BigDecimal amount, boolean balanced) {
    }

    record OpenChargebackRequest(@Positive long paymentId, @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String reason) {
    }
}
