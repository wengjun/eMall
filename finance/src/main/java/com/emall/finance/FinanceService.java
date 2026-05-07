package com.emall.finance;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FinanceService {
    private final FinanceRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    FinanceService(FinanceRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    FinanceAccount createAccount(AccountType accountType, long ownerId, String currency) {
        Instant now = Instant.now();
        return repository.saveAccount(new FinanceAccount(idGenerator.nextId(), accountType, ownerId,
                normalize(currency).toUpperCase(Locale.ROOT), BigDecimal.ZERO, BigDecimal.ZERO, now, now));
    }

    @Transactional
    LedgerEntry postEntry(long accountId, String businessType, String businessNo, BigDecimal debitAmount,
            BigDecimal creditAmount) {
        FinanceAccount account = requireAccount(accountId);
        if (debitAmount.signum() < 0 || creditAmount.signum() < 0 || debitAmount.add(creditAmount).signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ledger amount must be positive");
        }
        BigDecimal delta = creditAmount.subtract(debitAmount);
        repository.saveAccount(account.apply(delta));
        return repository.saveEntry(new LedgerEntry(idGenerator.nextId(), accountId, normalize(businessType),
                normalize(businessNo), debitAmount, creditAmount, Instant.now()));
    }

    @Transactional
    FinanceAccount freezeFunds(long accountId, BigDecimal amount) {
        FinanceAccount account = requireAccount(accountId);
        if (amount.signum() <= 0 || account.balance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "freezable balance is insufficient");
        }
        return repository.saveAccount(account.freeze(amount));
    }

    @Transactional
    SettlementBatch createSettlementBatch(long merchantId, BigDecimal amount, BigDecimal commissionAmount,
            LocalDate settlementDate) {
        Instant now = Instant.now();
        return repository.saveSettlementBatch(new SettlementBatch(idGenerator.nextId(), merchantId, amount,
                commissionAmount, SettlementStatus.CREATED, settlementDate, now, now));
    }

    @Transactional
    SettlementBatch changeSettlementStatus(long batchId, SettlementStatus status) {
        SettlementBatch batch = repository.findSettlementBatch(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "settlement batch not found"));
        return repository.saveSettlementBatch(batch.changeStatus(status));
    }

    @Transactional
    InvoiceDocument issueInvoice(long ownerId, BigDecimal amount, String taxNo) {
        Instant now = Instant.now();
        return repository.saveInvoice(new InvoiceDocument(idGenerator.nextId(), ownerId, amount, normalize(taxNo),
                InvoiceStatus.ISSUED, now, now));
    }

    @Transactional
    InvoiceDocument reconcileInvoice(long invoiceId, InvoiceStatus status) {
        InvoiceDocument invoice = repository.findInvoice(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "invoice not found"));
        return repository.saveInvoice(invoice.changeStatus(status));
    }

    @Transactional
    ClearingFile recordClearingFile(String channel, LocalDate clearingDate, BigDecimal amount, boolean balanced) {
        return repository.saveClearingFile(new ClearingFile(idGenerator.nextId(), normalize(channel), clearingDate,
                amount, balanced, Instant.now()));
    }

    @Transactional
    ChargebackCase openChargeback(long paymentId, BigDecimal amount, String reason) {
        Instant now = Instant.now();
        return repository.saveChargeback(
                new ChargebackCase(idGenerator.nextId(), paymentId, amount, reason, ChargebackStatus.OPEN, now, now));
    }

    FinanceSummary summary() {
        BigDecimal totalBalance = repository.findAccounts().stream().map(FinanceAccount::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFrozen = repository.findAccounts().stream().map(FinanceAccount::frozenAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int openChargebacks = (int) repository.findChargebacks().stream()
                .filter(chargeback -> chargeback.status() == ChargebackStatus.OPEN).count();
        return new FinanceSummary(totalBalance, totalFrozen, 0, openChargebacks);
    }

    private FinanceAccount requireAccount(long accountId) {
        return repository.findAccount(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "finance account not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "finance value must not be blank");
        }
        return normalized;
    }
}
