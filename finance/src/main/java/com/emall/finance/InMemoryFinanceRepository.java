package com.emall.finance;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryFinanceRepository implements FinanceRepository {
    private final ConcurrentMap<Long, FinanceAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, LedgerEntry> entries = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SettlementBatch> batches = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, InvoiceDocument> invoices = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ClearingFile> clearingFiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ChargebackCase> chargebacks = new ConcurrentHashMap<>();

    @Override
    public FinanceAccount saveAccount(FinanceAccount account) {
        accounts.put(account.accountId(), account);
        return account;
    }

    @Override
    public Optional<FinanceAccount> findAccount(long accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public List<FinanceAccount> findAccounts() {
        return List.copyOf(accounts.values());
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        entries.put(entry.entryId(), entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findEntries(long accountId) {
        return entries.values().stream().filter(entry -> entry.accountId() == accountId).toList();
    }

    @Override
    public SettlementBatch saveSettlementBatch(SettlementBatch batch) {
        batches.put(batch.batchId(), batch);
        return batch;
    }

    @Override
    public Optional<SettlementBatch> findSettlementBatch(long batchId) {
        return Optional.ofNullable(batches.get(batchId));
    }

    @Override
    public List<SettlementBatch> findSettlementBatches(long merchantId) {
        return batches.values().stream().filter(batch -> batch.merchantId() == merchantId).toList();
    }

    @Override
    public InvoiceDocument saveInvoice(InvoiceDocument invoice) {
        invoices.put(invoice.invoiceId(), invoice);
        return invoice;
    }

    @Override
    public Optional<InvoiceDocument> findInvoice(long invoiceId) {
        return Optional.ofNullable(invoices.get(invoiceId));
    }

    @Override
    public ClearingFile saveClearingFile(ClearingFile file) {
        clearingFiles.put(file.clearingFileId(), file);
        return file;
    }

    @Override
    public ChargebackCase saveChargeback(ChargebackCase chargeback) {
        chargebacks.put(chargeback.chargebackId(), chargeback);
        return chargeback;
    }

    @Override
    public Optional<ChargebackCase> findChargeback(long chargebackId) {
        return Optional.ofNullable(chargebacks.get(chargebackId));
    }

    @Override
    public List<ChargebackCase> findChargebacks() {
        return List.copyOf(chargebacks.values());
    }
}
