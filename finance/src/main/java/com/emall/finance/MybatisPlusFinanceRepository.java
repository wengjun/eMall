package com.emall.finance;

import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.localDateValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusFinanceRepository implements FinanceRepository {
    private final FinanceMapper financeMapper;

    MybatisPlusFinanceRepository(FinanceMapper financeMapper) {
        this.financeMapper = financeMapper;
    }

    @Override
    public FinanceAccount saveAccount(FinanceAccount account) {
        financeMapper.saveAccount(account);
        return account;
    }

    @Override
    public Optional<FinanceAccount> findAccount(long accountId) {
        return Optional.ofNullable(financeMapper.findAccount(accountId)).map(this::mapAccount);
    }

    @Override
    public List<FinanceAccount> findAccounts() {
        return financeMapper.findAccounts().stream().map(this::mapAccount).toList();
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        financeMapper.saveEntry(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findEntries(long accountId) {
        return financeMapper.findEntries(accountId).stream().map(this::mapEntry).toList();
    }

    @Override
    public SettlementBatch saveSettlementBatch(SettlementBatch batch) {
        financeMapper.saveSettlementBatch(batch);
        return batch;
    }

    @Override
    public Optional<SettlementBatch> findSettlementBatch(long batchId) {
        return Optional.ofNullable(financeMapper.findSettlementBatch(batchId)).map(this::mapSettlementBatch);
    }

    @Override
    public List<SettlementBatch> findSettlementBatches(long merchantId) {
        return financeMapper.findSettlementBatches(merchantId).stream().map(this::mapSettlementBatch).toList();
    }

    @Override
    public InvoiceDocument saveInvoice(InvoiceDocument invoice) {
        financeMapper.saveInvoice(invoice);
        return invoice;
    }

    @Override
    public Optional<InvoiceDocument> findInvoice(long invoiceId) {
        return Optional.ofNullable(financeMapper.findInvoice(invoiceId)).map(this::mapInvoice);
    }

    @Override
    public ClearingFile saveClearingFile(ClearingFile file) {
        financeMapper.saveClearingFile(file);
        return file;
    }

    @Override
    public ChargebackCase saveChargeback(ChargebackCase chargeback) {
        financeMapper.saveChargeback(chargeback);
        return chargeback;
    }

    @Override
    public Optional<ChargebackCase> findChargeback(long chargebackId) {
        return Optional.ofNullable(financeMapper.findChargeback(chargebackId)).map(this::mapChargeback);
    }

    @Override
    public List<ChargebackCase> findChargebacks() {
        return financeMapper.findChargebacks().stream().map(this::mapChargeback).toList();
    }

    private FinanceAccount mapAccount(Map<String, Object> row) {
        return new FinanceAccount(longValue(row, "account_id"),
                AccountType.valueOf(stringValue(row, "account_type")), longValue(row, "owner_id"),
                stringValue(row, "currency"), decimalValue(row, "balance"), decimalValue(row, "frozen_amount"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private LedgerEntry mapEntry(Map<String, Object> row) {
        return new LedgerEntry(longValue(row, "entry_id"), longValue(row, "account_id"),
                stringValue(row, "business_type"), stringValue(row, "business_no"), decimalValue(row, "debit_amount"),
                decimalValue(row, "credit_amount"), instantValue(row, "created_at"));
    }

    private SettlementBatch mapSettlementBatch(Map<String, Object> row) {
        return new SettlementBatch(longValue(row, "batch_id"), longValue(row, "merchant_id"),
                decimalValue(row, "amount"), decimalValue(row, "commission_amount"),
                SettlementStatus.valueOf(stringValue(row, "status")), localDateValue(row, "settlement_date"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private InvoiceDocument mapInvoice(Map<String, Object> row) {
        return new InvoiceDocument(longValue(row, "invoice_id"), longValue(row, "owner_id"),
                decimalValue(row, "amount"), stringValue(row, "tax_no"), InvoiceStatus.valueOf(stringValue(row,
                        "status")), instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private ChargebackCase mapChargeback(Map<String, Object> row) {
        return new ChargebackCase(longValue(row, "chargeback_id"), longValue(row, "payment_id"),
                decimalValue(row, "amount"), stringValue(row, "reason"),
                ChargebackStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }
}
